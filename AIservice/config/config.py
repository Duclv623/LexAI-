import os
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Đường dẫn dữ liệu
DATA_TO_ADD_PATH = os.path.join(BASE_DIR, "data", "docx_input")
DATA_FINAL_PATH = os.path.join(BASE_DIR, "data", "docx_processed")

# ChromaDB
CHROMA_PATH = os.path.join(BASE_DIR, "chroma_db")
COLLECTION_NAME = "phapluat"

# Embedding model
EMBEDDING_MODEL = "intfloat/multilingual-e5-large"

# LLM — Gemini (default)
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
LLM_MODEL_NAME = os.getenv("LLM_MODEL_NAME", "gemini-2.5-flash")

# LLM — Groq
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_MODEL_NAME = os.getenv("GROQ_MODEL_NAME", "llama-3.3-70b-versatile")

# JWT (zero-trust) — verify token bằng public key lấy từ JWKS của auth-service.
# ai-service KHÔNG giữ secret nào, chỉ fetch public key.
JWKS_URL = os.getenv("JWKS_URL", "http://localhost:8081/.well-known/jwks.json")
JWT_ISSUER = os.getenv("JWT_ISSUER", "http://localhost:8081")
