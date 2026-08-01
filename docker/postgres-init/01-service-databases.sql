-- Database-per-service: mỗi service Java có DB riêng và user riêng.
--
-- Chỉ tách database là chưa đủ: nếu cả hai service cùng đăng nhập bằng một account
-- thì ranh giới chỉ nằm trên giấy, service này vẫn nối sang DB service kia được.
-- Có user riêng + REVOKE CONNECT thì chính Postgres từ chối, không cần ai nhớ kỷ luật.
--
-- LƯU Ý: file trong /docker-entrypoint-initdb.d CHỈ chạy khi volume dữ liệu còn trống.
-- Với volume đã có sẵn dữ liệu, phải chạy tay các lệnh này một lần.

-- ---------- auth-service ----------
CREATE USER auth_service WITH PASSWORD 'auth_service';
CREATE DATABASE chatbox_auth OWNER auth_service;

REVOKE CONNECT ON DATABASE chatbox_auth FROM PUBLIC;
GRANT CONNECT ON DATABASE chatbox_auth TO auth_service;

-- ---------- chat-service ----------
CREATE USER chat_service WITH PASSWORD 'chat_service';
CREATE DATABASE chatbox_chat OWNER chat_service;

REVOKE CONNECT ON DATABASE chatbox_chat FROM PUBLIC;
GRANT CONNECT ON DATABASE chatbox_chat TO chat_service;
