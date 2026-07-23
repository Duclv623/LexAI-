import time
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from api.auth import require_user
from api.schemas import RagRequest, RagResponse
from api.rag_chain import run_rag
from api.retriever import get_vectorstore

logger = logging.getLogger("ai-service")
logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Warm up: load model E5 + Chroma khi start, không phải lần request đầu
    logger.info("Đang khởi tạo vectorstore + model embedding...")
    get_vectorstore()
    logger.info("AI service sẵn sàng.")
    yield


app = FastAPI(
    title="Legal Chatbot AI Service",
    description="RAG service trả lời câu hỏi pháp luật",
    version="0.1.0",
    lifespan=lifespan,
)

# Cho phép BE NestJS (chạy localhost:3000) gọi vào
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    try:
        count = get_vectorstore()._collection.count()
        return {"status": "ok", "chunks_in_db": count}
    except Exception as e:
        return {"status": "error", "detail": str(e)}


def _run_and_wrap(req: RagRequest, provider: str) -> RagResponse:
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="question không được để trống")

    start = time.time()
    try:
        result = run_rag(
            question=req.question,
            history=req.history,
            top_k=req.top_k,
            provider=provider,
        )
    except Exception as e:
        logger.exception(f"RAG pipeline ({provider}) lỗi")
        raise HTTPException(status_code=500, detail=str(e))

    latency_ms = int((time.time() - start) * 1000)
    logger.info(f"RAG [{provider}] xong trong {latency_ms}ms — câu hỏi: {req.question[:50]}...")

    return RagResponse(
        answer=result["answer"],
        citations=result["citations"],
        retrieved_chunks=result["retrieved_chunks"],
        latency_ms=latency_ms,
    )


@app.post("/rag", response_model=RagResponse)
def rag_endpoint(req: RagRequest, user: dict = Depends(require_user)):
    # Giữ endpoint cũ để không break BE — mặc định Gemini
    logger.info("RAG request từ user id=%s", user["id"])
    return _run_and_wrap(req, provider="gemini")


@app.post("/rag/gemini", response_model=RagResponse)
def rag_gemini(req: RagRequest, user: dict = Depends(require_user)):
    return _run_and_wrap(req, provider="gemini")


@app.post("/rag/groq", response_model=RagResponse)
def rag_groq(req: RagRequest, user: dict = Depends(require_user)):
    return _run_and_wrap(req, provider="groq")
