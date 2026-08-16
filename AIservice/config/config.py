import os
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Model embedding đã nằm sẵn trong cache HuggingFace và không bao giờ đổi, nên không
# có lý do gì phải gọi mạng lúc khởi động. Mặc định thư viện vẫn gửi HEAD lên
# huggingface.co để đối chiếu phiên bản — mất mạng một cái là nó retry 5 lần rồi làm
# chết cả service, dù bản cache nằm ngay trên đĩa.
# Dùng setdefault để vẫn tắt được bằng biến môi trường khi cần đổi model.
os.environ.setdefault("HF_HUB_OFFLINE", "1")

# Đường dẫn dữ liệu
DATA_TO_ADD_PATH = os.path.join(BASE_DIR, "data", "docx_input")
DATA_FINAL_PATH = os.path.join(BASE_DIR, "data", "docx_processed")

# Kho vector ChromaDB
CHROMA_PATH = os.path.join(BASE_DIR, "chroma_db")
COLLECTION_NAME = "phapluat"

# Mô hình sinh vector nhúng
EMBEDDING_MODEL = "intfloat/multilingual-e5-large"

# LLM — Gemini là mô hình duy nhất được dùng.
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
LLM_MODEL_NAME = os.getenv("LLM_MODEL_NAME", "gemini-3.5-flash-lite")

# JWT (zero-trust) — verify token bằng public key lấy từ JWKS của auth-service.
# ai-service KHÔNG giữ secret nào, chỉ fetch public key.
JWKS_URL = os.getenv("JWKS_URL", "http://localhost:8081/.well-known/jwks.json")
JWT_ISSUER = os.getenv("JWT_ISSUER", "http://localhost:8081")
