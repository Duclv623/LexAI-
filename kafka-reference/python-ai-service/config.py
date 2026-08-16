import os
from dotenv import load_dotenv

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

load_dotenv()

GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
PINECONE_API_KEY = os.getenv("PINECONE_API_KEY")
KAFKA_SERVER = os.getenv("KAFKA_SERVER")
QUESTION_TOPIC = os.getenv("QUESTION_TOPIC")
ANSWER_TOPIC = os.getenv("ANSWER_TOPIC")

EMBEDDING_MODEL_NAME = "intfloat/multilingual-e5-large"
LOCAL_EMBEDDING_MODEL_PATH = os.path.join(BASE_DIR, "embed_model")
LLM_MODEL_NAME = "gemini-1.5-flash"
DEVICE = "cpu" # or "cuda"

VECTOR_DB_TYPE = os.getenv("VECTOR_DB_TYPE", "pinecone").lower()

PINECONE_INDEX_NAME = "jurisaiv1"

CHROMA_DB_PATH = os.path.join(BASE_DIR, "db")

DATA_TO_ADD_PATH = os.path.join(BASE_DIR, "data_add")
DATA_TO_DELETE_PATH = os.path.join(BASE_DIR, "data_delete")
DATA_FINAL_PATH = os.path.join(BASE_DIR, "data_final")
