-- Database-per-service: mỗi service Java có DB riêng và user riêng.
--
-- Chỉ tách database là chưa đủ: nếu cả hai service cùng đăng nhập bằng một account
-- thì ranh giới chỉ nằm trên giấy, service này vẫn nối sang DB service kia được.
-- Có user riêng + REVOKE CONNECT thì chính Postgres từ chối, không cần ai nhớ kỷ luật.
--
-- LƯU Ý: file trong /docker-entrypoint-initdb.d CHỈ chạy khi volume dữ liệu còn trống.
-- Với volume đã có sẵn dữ liệu, phải chạy tay các lệnh này một lần.

-- psql \gexec giup script idempotent: dung duoc ca luc khoi tao volume moi va luc
-- database-init chay lai tren volume da ton tai.

-- ---------- auth-service ----------
SELECT 'CREATE USER auth_service WITH PASSWORD ''auth_service'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'auth_service') \gexec
ALTER USER auth_service WITH PASSWORD 'auth_service';

SELECT 'CREATE DATABASE chatbox_auth OWNER auth_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chatbox_auth') \gexec
ALTER DATABASE chatbox_auth OWNER TO auth_service;

REVOKE CONNECT ON DATABASE chatbox_auth FROM PUBLIC;
GRANT CONNECT ON DATABASE chatbox_auth TO auth_service;

-- ---------- chat-service ----------
SELECT 'CREATE USER chat_service WITH PASSWORD ''chat_service'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'chat_service') \gexec
ALTER USER chat_service WITH PASSWORD 'chat_service';

SELECT 'CREATE DATABASE chatbox_chat OWNER chat_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chatbox_chat') \gexec
ALTER DATABASE chatbox_chat OWNER TO chat_service;

REVOKE CONNECT ON DATABASE chatbox_chat FROM PUBLIC;
GRANT CONNECT ON DATABASE chatbox_chat TO chat_service;
