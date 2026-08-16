# Mã Kafka tham khảo

Gốc: https://github.com/Duclv623/ChatBot_AI (dự án JurisAI).

**Thư mục này KHÔNG được biên dịch và KHÔNG ảnh hưởng gì tới hệ thống hiện tại.**
Nó nằm ngoài mọi service nên Maven không quét tới. Toàn bộ mã HTTP đồng bộ đang
chạy vẫn giữ nguyên, chưa sửa một dòng nào.

## Hai topic

| Topic | Bên gửi | Bên nhận |
|---|---|---|
| `questions_topic` | chat-service (Java) | ai-service (Python) |
| `answers_topic` | ai-service (Python) | chat-service (Java) |

## Cấu trúc

```
kafka-reference/
├── chat-service/                      ← ĐÃ SỬA, copy được thẳng vào dự án
│   └── com/chatboxai/chat_service/
│       ├── config/KafkaProducerConfig.java
│       ├── config/KafkaConsumerConfig.java
│       ├── config/KafkaTopicConfig.java
│       └── util/kafka/KafkaMessageProducer.java
│                       KafkaMessageListener.java
├── application-kafka-snippet.yaml     đoạn cần chèn vào application.yaml
├── docker-compose-kafka.yml           Kafka KRaft 1 node
├── python-ai-service/                 mã Python, giữ nguyên bản gốc
└── original/                          bản gốc chưa sửa, để đối chiếu
```

Thư mục `chat-service/` giữ đúng đường dẫn package, nên copy nội dung của nó
đè vào `backend-spring-services/chat-service/src/main/java/` là các file rơi
đúng chỗ.

## Đã sửa những gì so với bản gốc

| File | Thay đổi |
|---|---|
| Tất cả | `package com.jurisai.*` → `com.chatboxai.chat_service.*`, sắp lại import |
| `KafkaConsumerConfig` | **Sửa lỗi kiểu dữ liệu**: bản gốc khai `<Integer, String>` nhưng đặt `StringDeserializer` cho khoá. Nay thống nhất `<String, String>` |
| `KafkaConsumerConfig` | Thêm `group-id` và `auto-offset-reset` vào map cấu hình |
| `KafkaTopicConfig` | Bỏ bean rác `topic1`, khai báo đúng 2 topic thật, đặt `partitions(3)` |
| `KafkaMessageProducer` | Thêm bản `send(topic, key, message)` — bản gốc không có khoá nên mất thứ tự |
| `KafkaMessageProducer` | Thêm callback ghi log khi gửi thất bại; bản gốc gửi xong không kiểm tra gì |
| `KafkaMessageListener` | Bỏ phụ thuộc `MessagesService` (bên này chưa có). Tạm ghi log, đánh dấu TODO |
| `KafkaMessageListener` | Tên thuộc tính `kafka.topic.answers_topic` → `app.kafka.topic.answers` cho khớp quy ước `app.*` của dự án |

## Lắp vào thì làm gì

1. Copy `chat-service/com` vào `backend-spring-services/chat-service/src/main/java/`
2. Chèn nội dung `application-kafka-snippet.yaml` vào `application.yaml` của chat-service
3. Thêm service `kafka` vào `docker-compose.yml` (xem `docker-compose-kafka.yml`)
4. **KHÔNG copy dependency từ `original/pom.xml.txt`** — bản gốc ghim cứng
   `spring-kafka:3.3.8`, kéo vào dự án Spring Boot 4 là xung đột. Pom của chat-service
   đã có sẵn `spring-boot-starter-kafka`, Boot 4 tự quản version.

Sau 3 bước trên là biên dịch và chạy được, listener sẽ ghi log mỗi khi có bản tin.
Chưa có bên nào gửi bản tin cả — muốn thử thì dùng `python-ai-service/kafka_producer_test.py`.

## Những gì bản tham khảo này chưa có

Chống trùng lặp, thử lại có khoảng chờ tăng dần, topic chứa bản tin chết, xác nhận
vị trí đọc thủ công, truyền danh tính người dùng, dừng tiến trình an toàn, và bài
toán ghi kép. Xem mục 5 của `KeHoach_Kafka_ChatBox2AI.docx`.

`original/MessagesController.java.txt` để nguyên dạng `.txt` vì nó phụ thuộc vào
hàng loạt lớp riêng của JurisAI (`MessagePK`, `Conversations`, `MessageMapper`…),
không port sang được. Chỉ đọc để xem chỗ nào gọi producer.
