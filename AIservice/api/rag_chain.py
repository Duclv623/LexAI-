import json
import re
from typing import List, Optional

from langchain_core.documents import Document
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
from langchain_google_genai import ChatGoogleGenerativeAI

from config.config import GOOGLE_API_KEY, LLM_MODEL_NAME
from api.retriever import retrieve
from api.schemas import ChatMessage, Citation, RetrievedChunk


_llm: Optional[BaseChatModel] = None


def get_llm() -> BaseChatModel:
    """
    Singleton LLM (Gemini).

    Giữ lại một instance thay vì tạo mới mỗi request: khởi tạo client kéo theo
    thiết lập kết nối và đọc cấu hình, làm lại mỗi lần là phí.
    """
    global _llm

    if _llm is None:
        if not GOOGLE_API_KEY:
            raise ValueError("GOOGLE_API_KEY không được tìm thấy trong .env")
        _llm = ChatGoogleGenerativeAI(
            model=LLM_MODEL_NAME,
            temperature=0.2,
            google_api_key=GOOGLE_API_KEY,
        )

    return _llm


def format_docs(docs: List[Document]) -> str:
    """Định dạng chunks cho LLM, đánh số từng nguồn."""
    lines = []

    for i, doc in enumerate(docs, start=1):
        content = doc.page_content or ""

        # Bỏ prefix "passage: " nếu có
        if content.startswith("passage: "):
            content = content[len("passage: "):]

        lines.append(
            f"--- Nguồn {i} ---\n"
            f"Tên văn bản: {doc.metadata.get('document_name', 'N/A')}\n"
            f"Văn bản gốc: {doc.metadata.get('source', 'N/A')}\n"
            f"Chương: {doc.metadata.get('chapter', '0')}\n"
            f"Mục: {doc.metadata.get('section', '0')}\n"
            f"Điều: {doc.metadata.get('article', '0')}\n"
            f"Nội dung: {content}"
        )

    return "\n\n".join(lines)


SYSTEM_PROMPT = """Bạn là AI trợ lý pháp lý Việt Nam. Bạn chỉ được trả lời dựa trên NGỮ CẢNH được cung cấp.

Quy tắc bắt buộc:
1. Chỉ dựa vào NGỮ CẢNH, không dùng kiến thức bên ngoài.
2. Nếu NGỮ CẢNH không đủ thông tin để trả lời, hãy nói rõ: "Chưa tìm thấy căn cứ phù hợp trong dữ liệu để trả lời câu hỏi này."
3. Câu trả lời phải ngắn gọn, chính xác, dễ hiểu, không xuống dòng.
4. Chỉ trích dẫn những nguồn thật sự được dùng để trả lời.
5. Không được tự tạo tên văn bản, chương, mục, điều hoặc source_file không có trong NGỮ CẢNH.
6. Nếu không đủ căn cứ, citations phải là [].
7. Bắt buộc trả về đúng JSON hợp lệ, không markdown, không giải thích ngoài JSON.

Định dạng JSON bắt buộc:
{
  "content": "câu trả lời ở đây",
  "citations": [
    {
      "law_name": "Tên văn bản",
      "chapter": "số chương",
      "section": "số mục",
      "article": "số điều",
      "source_file": "tên file"
    }
  ]
}
"""


def build_messages(
    question: str,
    context: str,
    history: Optional[List[ChatMessage]] = None,
):
    """Build messages đưa vào LLM."""
    messages = [SystemMessage(content=SYSTEM_PROMPT)]

    # Chỉ lấy vài message gần nhất để tránh nhiễu context
    if history:
        for m in history[-6:]:
            if m.role == "user":
                messages.append(HumanMessage(content=m.content))
            elif m.role == "assistant":
                messages.append(AIMessage(content=m.content))

    user_content = (
        f"NGỮ CẢNH:\n{context}\n\n"
        f"CÂU HỎI: {question}"
    )

    messages.append(HumanMessage(content=user_content))
    return messages


def _coerce_to_text(raw) -> str:
    """response.content có thể là str hoặc list[ContentPart] tùy version SDK."""
    if isinstance(raw, str):
        return raw
    if isinstance(raw, list):
        parts = []
        for p in raw:
            if isinstance(p, str):
                parts.append(p)
            elif isinstance(p, dict):
                parts.append(p.get("text", ""))
            else:
                parts.append(str(p))
        return "".join(parts)
    return str(raw) if raw is not None else ""


def _safe_parse_json(text: str) -> dict:
    """Parse JSON từ output LLM, có fallback nếu lỗi."""
    if not text:
        return {
            "content": "Không nhận được phản hồi từ mô hình.",
            "citations": [],
        }

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Thử extract JSON object từ text nếu LLM trả kèm markdown/text
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass

    return {
        "content": text,
        "citations": [],
    }


def _build_doc_lookup(docs: List[Document]) -> dict:
    """
    Map (source, article) → metadata của doc thật.
    Match mềm: chỉ cần LLM trỏ đúng (source_file, article) là chấp nhận;
    chapter/section/law_name lấy lại từ metadata gốc để tránh LLM bịa hoặc đổi format.
    """
    lookup = {}

    for d in docs:
        source = str(d.metadata.get("source", "")).strip().lower()
        article = str(d.metadata.get("article", "0")).strip()

        if not source:
            continue

        key = (source, article)
        # Giữ doc đầu tiên gặp cho mỗi key
        if key not in lookup:
            lookup[key] = d.metadata

    return lookup


def _filter_citations(citations_raw, docs: List[Document]) -> List[Citation]:
    """
    Lọc citations LLM trả về, chỉ giữ citation thật sự có trong retrieved docs.

    Match mềm (source_file + article). Sau khi xác nhận có trong nguồn,
    các trường law_name/chapter/section lấy lại từ metadata doc để chuẩn hóa,
    tránh trường hợp LLM viết "Điều 25" thay vì "25", hoặc đổi tên văn bản.
    """
    citations: List[Citation] = []

    if not isinstance(citations_raw, list):
        return citations

    lookup = _build_doc_lookup(docs)
    seen = set()

    for c in citations_raw:
        if not isinstance(c, dict):
            continue

        source_raw = str(c.get("source_file", "")).strip().lower()
        article_raw = str(c.get("article", "0")).strip()

        # Bỏ "Điều " prefix nếu LLM tự thêm
        article_norm = re.sub(r"(?i)^điều\s*", "", article_raw).strip() or "0"

        key = (source_raw, article_norm)
        if key not in lookup:
            continue
        if key in seen:
            continue
        seen.add(key)

        meta = lookup[key]
        citations.append(
            Citation(
                law_name=str(meta.get("document_name", "")),
                chapter=str(meta.get("chapter", "0")),
                section=str(meta.get("section", "0")),
                article=str(meta.get("article", "0")),
                source_file=str(meta.get("source", "")),
            )
        )

    return citations


def _build_retrieved_chunks(docs: List[Document]) -> List[RetrievedChunk]:
    """Build retrieved chunks trả về frontend/debug."""
    chunks = []

    for d in docs:
        content = d.page_content or ""

        chunks.append(
            RetrievedChunk(
                content=content.removeprefix("passage: "),
                metadata=d.metadata,
            )
        )

    return chunks


def run_rag(
    question: str,
    history: Optional[List[ChatMessage]] = None,
    top_k: int = 5,
):
    """Chạy RAG pipeline đầy đủ → trả về dict {answer, citations, retrieved_chunks}."""

    # 1. Retrieve tài liệu liên quan
    docs = retrieve(question, top_k=top_k)

    # 2. Format context (rỗng nếu không có docs — LLM sẽ tự từ chối theo system prompt)
    context = format_docs(docs)

    # 3. Gọi LLM
    llm = get_llm()
    messages = build_messages(question, context, history)

    try:
        response = llm.invoke(messages)
    except Exception as e:
        return {
            "answer": f"Lỗi khi gọi mô hình LLM: {str(e)}",
            "citations": [],
            "retrieved_chunks": _build_retrieved_chunks(docs),
        }

    # 4. Parse JSON output
    parsed = _safe_parse_json(_coerce_to_text(response.content))

    answer = parsed.get("content", "")
    if not answer:
        answer = "Chưa tìm thấy căn cứ phù hợp trong dữ liệu để trả lời câu hỏi này."

    # 5. Lọc citations hợp lệ (docs rỗng ⇒ lookup rỗng ⇒ citations sẽ bị lọc về [])
    citations_raw = parsed.get("citations", [])
    citations = _filter_citations(citations_raw, docs)

    # 6. Build chunks để trả về frontend/debug
    chunks = _build_retrieved_chunks(docs)

    return {
        "answer": answer,
        "citations": citations,
        "retrieved_chunks": chunks,
    }