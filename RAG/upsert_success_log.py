"""
upsert_success_log.py
======================
역할:
  - rag_success_log 테이블에서 is_upserted=0 인 성공 로그를 읽어
    Python RAG 서버 /api/v1/upsert 엔드포인트로 전송
  - 반영 성공 시 is_upserted=1 로 업데이트

실행 방법:
  # 수동 실행
  python upsert_success_log.py

  # Windows 작업 스케줄러 등록 (매주 월요일 오전 9시)
  schtasks /create /tn "RAG_Upsert" /tr "python C:\\path\\upsert_success_log.py" /sc weekly /d MON /st 09:00

의존성 설치:
  pip install mysql-connector-python requests python-dotenv

.env 파일 (Java 서버와 동일한 .env 재사용):
  DB_URL=jdbc:mysql://localhost:3306/hr_erp?...  <- 파싱해서 host/port/db 추출
  DB_USER=root
  DB_PASSWORD=1234
"""

import os
import re
import json
import logging
import requests
import mysql.connector
from dotenv import load_dotenv

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "..", ".env"))

RAG_SERVER_UPSERT_URL = "http://127.0.0.1:8000/api/v1/upsert"
REQUEST_TIMEOUT_SEC   = 5
BATCH_SIZE            = 100   # 1회 실행당 최대 처리 건수

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────
# DB 연결 설정 파싱
# Java .env의 JDBC URL을 파싱하여 재사용
# ──────────────────────────────────────────────
def get_db_config() -> dict:
    jdbc_url = os.getenv("DB_URL", "")

    # jdbc:mysql://localhost:3306/hr_erp?... 파싱
    match = re.match(r"jdbc:mysql://([^:/]+):(\d+)/([^?]+)", jdbc_url)
    if match:
        host, port, database = match.group(1), int(match.group(2)), match.group(3)
    else:
        host, port, database = "localhost", 3306, "hr_erp"

    return {
        "host":     host,
        "port":     port,
        "database": database,
        "user":     os.getenv("DB_USER",     "root"),
        "password": os.getenv("DB_PASSWORD", ""),
        "charset":  "utf8mb4"
    }


# ──────────────────────────────────────────────
# 카테고리 문자열 → 리스트 변환
# ──────────────────────────────────────────────
def parse_categories(category_str: str) -> list:
    """
    "ATTENDANCE,EMPLOYEE" → ["ATTENDANCE", "EMPLOYEE"]
    """
    return [c.strip() for c in category_str.split(",") if c.strip()]


# ──────────────────────────────────────────────
# 유니크 ID 생성
# ──────────────────────────────────────────────
def make_upsert_id(log_id: int) -> str:
    return f"log_{log_id:08d}"


# ──────────────────────────────────────────────
# RAG 서버 헬스체크
# ──────────────────────────────────────────────
def check_rag_server() -> bool:
    try:
        resp = requests.get("http://127.0.0.1:8000/health", timeout=3)
        return resp.status_code == 200
    except Exception:
        return False


# ──────────────────────────────────────────────
# 메인 배치 로직
# ──────────────────────────────────────────────
def run_batch():
    logger.info("=" * 50)
    logger.info("RAG 선순환 배치 시작")

    # ① RAG 서버 확인
    if not check_rag_server():
        logger.error("RAG 서버에 연결할 수 없습니다. 배치를 종료합니다.")
        return

    # ② DB 연결
    db_config = get_db_config()
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        logger.info(f"DB 연결 성공: {db_config['host']}:{db_config['port']}/{db_config['database']}")
    except Exception as e:
        logger.error(f"DB 연결 실패: {e}")
        return

    success_count = 0
    fail_count    = 0

    try:
        # ③ 미반영 로그 조회
        cursor.execute("""
            SELECT log_id, question, generated_sql, category, similarity
            FROM rag_success_log
            WHERE is_upserted = 0
            ORDER BY created_at ASC
            LIMIT %s
        """, (BATCH_SIZE,))
        rows = cursor.fetchall()

        if not rows:
            logger.info("처리할 미반영 로그가 없습니다.")
            return

        logger.info(f"처리 대상: {len(rows)}건")

        # ④ 각 로그를 RAG 서버에 upsert
        for row in rows:
            log_id   = row["log_id"]
            upsert_id = make_upsert_id(log_id)

            payload = {
                "id":       upsert_id,
                "category": parse_categories(row["category"]),
                "question": row["question"],
                "sql":      row["generated_sql"],
                "tags":     []
            }

            try:
                resp = requests.post(
                    RAG_SERVER_UPSERT_URL,
                    json=payload,
                    timeout=REQUEST_TIMEOUT_SEC
                )

                if resp.status_code == 200 and resp.json().get("success"):
                    # ⑤ 반영 완료 표시
                    cursor.execute(
                        "UPDATE rag_success_log SET is_upserted = 1 WHERE log_id = %s",
                        (log_id,)
                    )
                    conn.commit()
                    success_count += 1
                    logger.info(f"  ✅ log_id={log_id} upsert 완료 → '{row['question'][:30]}...'")

                else:
                    fail_count += 1
                    logger.warning(f"  ❌ log_id={log_id} upsert 실패: {resp.text[:100]}")

            except requests.Timeout:
                fail_count += 1
                logger.warning(f"  ⏱ log_id={log_id} 타임아웃 (건너뜀)")
            except Exception as e:
                fail_count += 1
                logger.warning(f"  ❌ log_id={log_id} 오류: {e}")

    finally:
        cursor.close()
        conn.close()

    logger.info("=" * 50)
    logger.info(f"배치 완료 — 성공: {success_count}건, 실패: {fail_count}건")
    logger.info("=" * 50)


if __name__ == "__main__":
    run_batch()
