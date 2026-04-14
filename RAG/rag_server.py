"""
rag_server.py
=============
역할:
  - Java 메인 서버로부터 HTTP POST 요청을 받아
    ChromaDB에서 유사 예제를 검색 후 반환하는 FastAPI 마이크로서비스

의존성 설치:
  pip install fastapi uvicorn

실행:
  # 기본 실행 (포트 8000)
  python rag_server.py

  # ChromaDB가 비어있으면 자동으로 seed_data.json rebuild 수행

API:
  POST /api/v1/search_context
  GET  /health
  POST /api/v1/upsert       (운영 로그 선순환 — Phase 3)
"""

import os
import sys
import logging
from contextlib import asynccontextmanager
from typing import List, Optional

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# vector_store.py가 같은 디렉토리에 있어야 함
sys.path.insert(0, os.path.dirname(__file__))
import vector_store

# ──────────────────────────────────────────────
# 로깅 설정
# ──────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────
# 서버 기동 시 초기화 (lifespan)
# ──────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """서버 시작 시 모델 & ChromaDB 사전 로드"""
    logger.info("=" * 50)
    logger.info("HR-ERP RAG 서버 초기화 시작")

    # ChromaDB가 비어있으면 자동 rebuild
    collection = vector_store._get_collection()
    if collection.count() == 0:
        logger.warning("ChromaDB가 비어있습니다. seed_data.json으로 자동 rebuild 시작...")
        vector_store.rebuild_collection()
    else:
        logger.info(f"ChromaDB 로드 완료 — 저장된 예제 수: {collection.count()}")

    # 임베딩 모델 워밍업 (첫 요청 지연 방지)
    logger.info("임베딩 모델 워밍업 중...")
    vector_store._get_model()
    logger.info("RAG 서버 초기화 완료 ✓")
    logger.info("=" * 50)

    yield  # 서버 실행 구간

    logger.info("RAG 서버 종료")


# ──────────────────────────────────────────────
# FastAPI 앱 생성
# ──────────────────────────────────────────────
app = FastAPI(
    title="HR-ERP RAG Server",
    description="Text-to-SQL 벡터 검색 마이크로서비스",
    version="1.0.0",
    lifespan=lifespan
)

# Java 서버(localhost)에서만 호출하므로 CORS는 로컬만 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)


# ──────────────────────────────────────────────
# 요청/응답 스키마
# ──────────────────────────────────────────────
class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=500, description="사용자 자연어 질문")
    top_k: int = Field(default=2, ge=1, le=5, description="반환할 예제 수")


class FewShotExample(BaseModel):
    question: str
    sql: str


class SearchResponse(BaseModel):
    matched_categories: List[str]
    few_shot_examples: List[FewShotExample]
    similarity_scores: List[float]


class UpsertRequest(BaseModel):
    id: str = Field(..., description="고유 ID (예: log_20250101_001)")
    category: List[str] = Field(..., description="카테고리 목록")
    question: str = Field(..., description="사용자 질문")
    sql: str = Field(..., description="실행 성공한 SQL")
    tags: Optional[List[str]] = Field(default=[], description="태그 목록")
    is_cross_domain: Optional[bool] = Field(default=False)


class UpsertResponse(BaseModel):
    success: bool
    id: str
    message: str


# ──────────────────────────────────────────────
# 엔드포인트
# ──────────────────────────────────────────────
@app.get("/health")
async def health_check():
    """Java 서버의 폴백 판단 기준 — 3초 내 200 응답 없으면 폴백"""
    try:
        collection = vector_store._get_collection()
        return {
            "status": "ok",
            "collection": vector_store.COLLECTION_NAME,
            "document_count": collection.count()
        }
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.post("/api/v1/search_context", response_model=SearchResponse)
async def search_context(req: SearchRequest):
    """
    Java 메인 서버 → 이 엔드포인트로 질문 전송
    유사 예제 top_k개와 카테고리를 반환

    Java 측 타임아웃: 3초 설정 권장
    similarity_scores 전부 < 0.4 이면 Java 측에서 예제 없이 BASE 프롬프트만 사용
    """
    logger.info(f"[검색] query='{req.query}', top_k={req.top_k}")

    try:
        result = vector_store.search(query=req.query, top_k=req.top_k)
    except Exception as e:
        logger.error(f"[검색 오류] {e}")
        raise HTTPException(status_code=500, detail=f"벡터 검색 오류: {str(e)}")

    logger.info(
        f"[검색 완료] categories={result['matched_categories']}, "
        f"scores={result['similarity_scores']}"
    )

    return SearchResponse(
        matched_categories=result["matched_categories"],
        few_shot_examples=[
            FewShotExample(question=ex["question"], sql=ex["sql"])
            for ex in result["few_shot_examples"]
        ],
        similarity_scores=result["similarity_scores"]
    )


@app.post("/api/v1/upsert", response_model=UpsertResponse)
async def upsert_example(req: UpsertRequest):
    """
    운영 로그 선순환 (Phase 3).
    성공한 쿼리를 벡터 DB에 추가하여 검색 품질을 점진적으로 향상.
    """
    logger.info(f"[Upsert] id='{req.id}', question='{req.question[:30]}...'")

    try:
        vector_store.upsert({
            "id":            req.id,
            "category":      req.category,
            "question":      req.question,
            "sql":           req.sql,
            "tags":          req.tags or [],
            "is_cross_domain": req.is_cross_domain or False
        })
    except Exception as e:
        logger.error(f"[Upsert 오류] {e}")
        raise HTTPException(status_code=500, detail=f"Upsert 오류: {str(e)}")

    return UpsertResponse(
        success=True,
        id=req.id,
        message=f"'{req.id}' 벡터 DB 적재 완료"
    )


@app.post("/api/v1/rebuild")
async def rebuild():
    """
    seed_data.json으로 ChromaDB 전체 재구축.
    예제 데이터 대규모 업데이트 시 사용.
    """
    logger.info("[Rebuild] ChromaDB 재구축 시작")
    try:
        vector_store.rebuild_collection()
        count = vector_store._get_collection().count()
        return {"success": True, "document_count": count}
    except Exception as e:
        logger.error(f"[Rebuild 오류] {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ──────────────────────────────────────────────
# 서버 진입점
# ──────────────────────────────────────────────
if __name__ == "__main__":
    uvicorn.run(
        "rag_server:app",
        host="127.0.0.1",   # 로컬 전용 (외부 노출 불필요)
        port=8000,
        reload=False,        # 운영 환경에서는 False
        log_level="info"
    )
