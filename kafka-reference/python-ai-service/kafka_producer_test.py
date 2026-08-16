import json
from kafka import KafkaProducer
import time

from config.config import KAFKA_SERVER, QUESTION_TOPIC

# Dữ liệu đầu vào ví dụ, giống như trong ảnh của bạn
payload = {
    "account_id": 1,
    "conversations_id": 1,
    "model_chat": "test1",
    "message_id": 103,
    "content": "Quyền lợi của người lao động khi nghỉ thai sản là gì?"
}

# Chuyển dictionary thành chuỗi JSON
message_str = json.dumps(payload)

print(f"Chuẩn bị gửi message đến topic '{QUESTION_TOPIC}':")
print(message_str)

# Khởi tạo producer
# Ở đây, value_serializer sẽ chuyển chuỗi string thành bytes
producer = KafkaProducer(
    bootstrap_servers=[KAFKA_SERVER],
    value_serializer=str.encode
)

# Gửi message
producer.send(QUESTION_TOPIC, value=message_str)

# Đảm bảo message được gửi đi trước khi script kết thúc
producer.flush()

print("\nĐã gửi message thành công!")