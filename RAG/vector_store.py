"""
vector_store.py
================
역할:
  - seed_data.json을 읽어 multilingual-e5-small로 임베딩
  - ChromaDB 컬렉션에 적재 (파일 기반 영속성)
  - RAG 서버(rag_server.py)에서 import하여 사용

의존성 설치:
  pip install chromadb sentence-transformers

사용법:
  # 최초 1회 또는 seed 데이터 갱신 시
  python vector_store.py --rebuild

  # 단순 모듈 import 시에는 자동으로 기존 컬렉션 로드
"""

import json
import os
import argparse
from typing import List, Dict, Any

from sentence_transformers import SentenceTransformer
import chromadb
from chromadb.config import Settings

# ──────────────────────────────────────────────
# 설정 상수
# ──────────────────────────────────────────────
SEED_DATA_PATH   = os.path.join(os.path.dirname(__file__), "seed_data.json")
CHROMA_DB_PATH   = os.path.join(os.path.dirname(__file__), "chroma_db")   # 파일 기반 영속성
COLLECTION_NAME  = "hr_erp_examples"
EMBEDDING_MODEL  = "intfloat/multilingual-e5-small"   # CPU 전용, 118M params
EMBED_DEVICE     = "cpu"                               # GPU는 Qwen에 양보


# ──────────────────────────────────────────────
# 싱글톤: 모델 & 클라이언트 (서버 기동 시 1회 로드)
# ──────────────────────────────────────────────
_model      = None
_chroma_client = None
_collection    = None


def _get_model() -> SentenceTransformer:
    """임베딩 모델 싱글톤 반환"""
    global _model
    if _model is None:
        print(f"[VectorStore] 임베딩 모델 로딩: {EMBEDDING_MODEL} (device={EMBED_DEVICE})")
        _model = SentenceTransformer(EMBEDDING_MODEL, device=EMBED_DEVICE)
        print("[VectorStore] 모델 로딩 완료")
    return _model


def _get_collection():
    """ChromaDB 컬렉션 싱글톤 반환"""
    global _chroma_client, _collection
    if _collection is None:
        _chroma_client = chromadb.PersistentClient(
            path=CHROMA_DB_PATH,
            settings=Settings(anonymized_telemetry=False)
        )
        _collection = _chroma_client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"}   # 코사인 유사도 사용
        )
        print(f"[VectorStore] 컬렉션 로드: '{COLLECTION_NAME}' "
              f"(저장된 문서 수: {_collection.count()})")
    return _collection


# ──────────────────────────────────────────────
# 임베딩 텍스트 조합 전략
# ──────────────────────────────────────────────
def _build_embed_text(item: Dict[str, Any]) -> str:
    """
    question + tags를 합쳐서 임베딩 품질 향상
    e5 모델은 'query: ' 접두사가 있을 때 검색 성능이 올라감
    (passage는 적재 시, query는 검색 시 적용)
    """
    tags_str = " ".join(item.get("tags", []))
    return f"passage: {item['question']} {tags_str}"


# ──────────────────────────────────────────────
# 초기 적재 (rebuild)
# ──────────────────────────────────────────────
def rebuild_collection() -> None:
    """
    seed_data.json 전체를 ChromaDB에 적재.
    기존 컬렉션은 삭제 후 재생성.
    """
    print(f"[VectorStore] seed 데이터 로딩: {SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data: List[Dict[str, Any]] = json.load(f)

    model      = _get_model()
    client     = chromadb.PersistentClient(
        path=CHROMA_DB_PATH,
        settings=Settings(anonymized_telemetry=False)
    )

    # 기존 컬렉션 삭제 후 재생성
    try:
        client.delete_collection(COLLECTION_NAME)
        print(f"[VectorStore] 기존 컬렉션 삭제: '{COLLECTION_NAME}'")
    except Exception:
        pass

    collection = client.create_collection(
        name=COLLECTION_NAME,
        metadata={"hnsw:space": "cosine"}
    )

    # 임베딩 대상 텍스트 조합
    ids       = [item["id"] for item in seed_data]
    texts     = [_build_embed_text(item) for item in seed_data]
    metadatas = [
        {
            "question":       item["question"],
            "sql":            item["sql"],
            "category":       ",".join(item["category"]),   # 리스트 → 문자열 (ChromaDB 제약)
            "is_cross_domain": str(item["is_cross_domain"]),
            "tags":           ",".join(item.get("tags", []))
        }
        for item in seed_data
    ]

    print(f"[VectorStore] {len(texts)}개 문서 임베딩 중...")
    embeddings = model.encode(texts, batch_size=32, show_progress_bar=True).tolist()

    collection.add(
        ids=ids,
        embeddings=embeddings,
        metadatas=metadatas,
        documents=texts
    )

    print(f"[VectorStore] 적재 완료: {collection.count()}개 문서")

    # 글로벌 싱글톤 갱신
    global _chroma_client, _collection
    _chroma_client = client
    _collection    = collection


# ──────────────────────────────────────────────
# 검색 (메인 인터페이스)
# ──────────────────────────────────────────────
def search(query: str, top_k: int = 2) -> Dict[str, Any]:
    """
    자연어 질문을 받아 유사한 예제를 ChromaDB에서 검색.

    Returns:
        {
            "matched_categories": ["ATTENDANCE", "EMPLOYEE"],
            "few_shot_examples": [
                {"question": "...", "sql": "..."},
                ...
            ],
            "similarity_scores": [0.92, 0.85]
        }
    """
    model      = _get_model()
    collection = _get_collection()

    # 검색 시에는 'query: ' 접두사 사용 (e5 권장 방식)
    query_text = f"query: {query}"
    query_vec  = model.encode([query_text]).tolist()[0]

    results = collection.query(
        query_embeddings=[query_vec],
        n_results=min(top_k, collection.count()),
        include=["metadatas", "distances"]
    )

    # ChromaDB는 거리(distance)를 반환 → 유사도로 변환 (cosine: 1 - distance)
    metadatas = results["metadatas"][0]   # 첫 번째 query 결과
    distances = results["distances"][0]

    matched_categories: List[str] = []
    few_shot_examples:  List[Dict[str, str]] = []
    similarity_scores:  List[float] = []

    for meta, dist in zip(metadatas, distances):
        similarity = round(1.0 - dist, 4)
        similarity_scores.append(similarity)

        # 카테고리 추출 (중복 제거, 순서 유지)
        for cat in meta["category"].split(","):
            cat = cat.strip()
            if cat and cat not in matched_categories:
                matched_categories.append(cat)

        few_shot_examples.append({
            "question": meta["question"],
            "sql":      meta["sql"]
        })

    return {
        "matched_categories": matched_categories,
        "few_shot_examples":  few_shot_examples,
        "similarity_scores":  similarity_scores
    }


# ──────────────────────────────────────────────
# upsert — 운영 로그 선순환 (Phase 3에서 사용)
# ──────────────────────────────────────────────
def upsert(item: Dict[str, Any]) -> None:
    """
    성공 쿼리 로그를 벡터 DB에 추가/갱신.
    id가 이미 존재하면 덮어씀.

    item 형식:
        {
            "id":       "log_20250101_001",
            "category": ["ATTENDANCE"],
            "question": "사용자 실제 질문",
            "sql":      "실행 성공한 SQL",
            "tags":     []
        }
    """
    model      = _get_model()
    collection = _get_collection()

    text      = _build_embed_text(item)
    embedding = model.encode([text]).tolist()[0]
    metadata  = {
        "question":        item["question"],
        "sql":             item["sql"],
        "category":        ",".join(item.get("category", ["EMPLOYEE"])),
        "is_cross_domain": str(item.get("is_cross_domain", False)),
        "tags":            ",".join(item.get("tags", []))
    }

    collection.upsert(
        ids=[item["id"]],
        embeddings=[embedding],
        metadatas=[metadata],
        documents=[text]
    )
    print(f"[VectorStore] upsert 완료: {item['id']}")


# ──────────────────────────────────────────────
# CLI 진입점
# ──────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="HR-ERP VectorStore 관리")
    parser.add_argument(
        "--rebuild",
        action="store_true",
        help="seed_data.json으로 ChromaDB 전체 재구축"
    )
    parser.add_argument(
        "--search",
        type=str,
        default=None,
        help="테스트 검색 쿼리 (예: --search '이번달 지각 많은 부장')"
    )
    parser.add_argument(
        "--top-k",
        type=int,
        default=2,
        help="검색 결과 수 (기본값: 2)"
    )
    args = parser.parse_args()

    if args.rebuild:
        rebuild_collection()

    if args.search:
        print(f"\n[테스트 검색] 질문: '{args.search}'")
        result = search(args.search, top_k=args.top_k)
        print(f"  매칭 카테고리  : {result['matched_categories']}")
        print(f"  유사도 점수    : {result['similarity_scores']}")
        print(f"  예제 수        : {len(result['few_shot_examples'])}")
        for i, ex in enumerate(result["few_shot_examples"], 1):
            print(f"\n  [{i}] Q: {ex['question']}")
            print(f"       A: {ex['sql'][:80]}...")
