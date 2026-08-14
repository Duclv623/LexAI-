# ChatBox2AI

ChatBox2AI la he thong chatbot hoi dap phap luat Viet Nam, ket hop giao dien web, backend API, microservices va AI service dung RAG + LLM de tra loi cau hoi dua tren kho tai lieu phap luat.

> Trang thai: du an dang phat trien, trong do phan microservice Spring Boot dang duoc xay dung them de tach API Gateway, Auth Service, Chat Service va co che giao tiep an toan bang JWT/JWKS.

## Tinh nang chinh

- Chat hoi dap phap luat Viet Nam voi RAG.
- Ho tro nhieu nha cung cap LLM: Gemini va Groq.
- Luu phien chat va tin nhan bang PostgreSQL thong qua Prisma.
- Dang ky, dang nhap va bao ve API bang JWT.
- AI service rieng bang FastAPI, ChromaDB va embedding model `intfloat/multilingual-e5-large`.
- Microservice Spring Boot gom API Gateway, Auth Service va Chat Service.
- Redis va PostgreSQL co san qua Docker Compose.

## Kien truc tong quan

```text
frontend/                 Next.js UI
backend/                  NestJS API, Prisma, auth, chat, sessions
AIservice/                FastAPI RAG service, ChromaDB, LLM providers
backend-spring-services/  Spring Boot microservices
DataChat/                 Du lieu/tai lieu cho chatbot
docker-compose.yml        PostgreSQL + Redis
```

Luon chay co ban:

```text
Browser -> frontend -> backend NestJS -> AIservice
                       |
                       -> PostgreSQL
```

Luon microservice dang phat trien:

```text
Browser/API client -> api-gateway :8080
                   -> auth-service :8081
                   -> chat-service :8082
                   -> AIservice :8000
                   -> Redis/PostgreSQL
```

## Yeu cau moi truong

- Node.js va npm
- Python 3.10+
- Java 21+ va Maven, hoac Maven Wrapper neu tung service co wrapper
- Docker Desktop hoac Docker Engine
- PostgreSQL va Redis neu khong dung Docker Compose

## Cau hinh moi truong

### PostgreSQL va Redis

Chi chay database/cache khi phat trien service truc tiep tren may:

```bash
docker compose up -d postgres redis database-init
```

Mac dinh `docker-compose.yml` tao:

```text
POSTGRES_USER=chatbox
POSTGRES_PASSWORD=chatbox
POSTGRES_DB=chatbox
REDIS_PORT=6379
POSTGRES_PORT=5432
```

Neu dung NestJS backend, can dam bao `backend/.env` co `DATABASE_URL` khop voi database dang chay, vi file hien tai co the khac voi cau hinh Docker Compose.

Vi du khi dung Docker Compose:

```env
DATABASE_URL="postgresql://chatbox:chatbox@localhost:5432/chatbox?schema=public"
AI_SERVICE_URL="http://localhost:8000"
AI_SERVICE_TIMEOUT_MS=60000
PORT=3001
JWT_SECRET="change-me-to-a-long-random-secret"
JWT_EXPIRES_IN="7d"
```

### Frontend

`frontend/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:3001
```

Neu chay qua Spring API Gateway, co the doi sang:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### AIservice

Tao `AIservice/.env` va dien khoa API cua provider ban dung:

```env
GOOGLE_API_KEY="your-google-api-key"
GROQ_API_KEY="your-groq-api-key"
LLM_MODEL_NAME="gemini-2.5-flash"
GROQ_MODEL_NAME="llama-3.3-70b-versatile"
JWKS_URL="http://localhost:8081/.well-known/jwks.json"
JWT_ISSUER="http://localhost:8081"
```

Khong commit API key that len repository.

## Cai dat va chay du an

### 1. Chay toan bo he thong bang Docker Compose

```bash
copy AIservice\.env.example AIservice\.env
# Dien GOOGLE_API_KEY trong AIservice/.env, sau do:
docker compose up -d --build
```

Tren Linux/macOS, thay `copy` bang `cp`. Chi Frontend va API Gateway duoc
publish ra may host:

```text
Frontend:    http://localhost:3000
API Gateway: http://localhost:8080
```

Auth Service, Chat Service, AI Service, PostgreSQL va Redis chi truy cap duoc
trong mang Docker. Moi API tu ben ngoai bat buoc di qua API Gateway.

Du lieu vector da embedding trong `AIservice/chroma_db` duoc dong goi truc tiep
vao AI image, khong xu ly lai tai lieu khi khoi dong. Lan dau container chay, model
E5 dung de embedding cau hoi co the duoc tai vao volume `hf-cache`; cac lan sau se
dung lai cache nay.

```bash
docker compose ps
docker compose logs -f
```

Dung he thong ma giu nguyen database va cache:

```bash
docker compose down
```

### 2. Chay tung thanh phan de phat trien

Neu khong dong goi cac service ung dung, chi khoi dong ha tang bang:

```bash
docker compose up -d postgres redis database-init
```

### 3. Chay AIservice truc tiep

Thu muc `AIservice` da co `requirements.txt` de cai dat dong nhat:

```bash
cd AIservice
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn api.main:app --reload --port 8000
```

Tren Windows PowerShell:

```powershell
cd AIservice
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn api.main:app --reload --port 8000
```

Kiem tra service:

```bash
curl http://localhost:8000/health
```

### 4. Chay frontend truc tiep

```bash
cd frontend
npm install
npm run dev
```

Frontend mac dinh chay tai:

```text
http://localhost:3000
```

## Chay cum Spring microservices

Trong `backend-spring-services` co 3 service:

- `api-gateway`: port `8080`
- `auth-service`: port `8081`
- `chat-service`: port `8082`

Chay tung service:

```bash
cd backend-spring-services/auth-service
mvn spring-boot:run
```

```bash
cd backend-spring-services/chat-service
mvn spring-boot:run
```

```bash
cd backend-spring-services/api-gateway
mvn spring-boot:run
```

API Gateway route:

```text
/api/auth/** -> auth-service
/api/chat/** -> chat-service
/api/ai/**   -> AIservice, strip prefix /api/ai
```

Co the test luong JWT/JWKS bang script:

```bash
cd backend-spring-services
bash test-jwt-flow.sh
```

## Script huu ich

Frontend:

```bash
npm run dev
npm run build
npm run start
npm run lint
```

NestJS backend:

```bash
npm run start:dev
npm run build
npm run start:prod
npm run test
npm run test:e2e
npm run lint
```

Prisma:

```bash
npx prisma migrate dev
npx prisma generate
npx prisma studio
```

## API chinh

NestJS backend gom cac module:

- `auth`: dang ky, dang nhap, doi mat khau, thong tin nguoi dung hien tai.
- `sessions`: quan ly phien chat.
- `chat`: gui cau hoi va nhan cau tra loi tu AI service.
- `ai-client`: client noi bo goi FastAPI RAG service.

AIservice:

- `GET /health`: kiem tra trang thai vector store.
- `POST /rag`: hoi dap RAG mac dinh bang Gemini.
- `POST /rag/gemini`: hoi dap bang Gemini.
- `POST /rag/groq`: hoi dap bang Groq.

## Ghi chu phat trien

- Thu muc `AIservice/chroma_db` la vector database cuc bo.
- Thu muc `AIservice/data` va `DataChat` dung cho du lieu/tai lieu phap luat.
- Nen tao `requirements.txt` cho `AIservice` de cai dat moi truong Python on dinh hon.
- Nen dua cac file `.env` that ra khoi git va chi commit file mau nhu `.env.example`.
- Khi doi cau hinh port hoac gateway, cap nhat dong thoi frontend env, backend env va YAML cua Spring services.

## License

Chua khai bao license.
