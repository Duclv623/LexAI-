import os
import json
from kafka import KafkaConsumer, KafkaProducer
import sys


sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import các hàm khởi tạo từ các module đã tách
from utils.vector_db import get_retriever
from utils.rag_chain import get_llm, create_rag_chain
from config.config import KAFKA_SERVER, QUESTION_TOPIC, ANSWER_TOPIC

# --- Cấu hình Kafka ---


def create_kafka_producer():
    """Tạo một Kafka Producer."""
    return KafkaProducer(
        bootstrap_servers=[KAFKA_SERVER],
        # Chuyển đổi giá trị sang dạng bytes được mã hóa utf-8
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode('utf-8')
    )


def main():
    """
    Hàm chính của service: khởi tạo RAG chain, lắng nghe Kafka và xử lý.
    """
    print("--- BẮT ĐẦU KHỞI TẠO HỆ THỐNG CHATBOT ---")

    # --- Bước 1, 2, 3: Khởi tạo toàn bộ hệ thống RAG ---
    retriever = get_retriever()
    llm = get_llm()
    rag_chain = create_rag_chain(retriever, llm)

    print("\n--- HỆ THỐNG ĐÃ SẴN SÀNG! ---")

    # --- Bước 4: Khởi tạo Kafka Consumer và Producer ---
    consumer = KafkaConsumer(
        QUESTION_TOPIC,
        bootstrap_servers=[KAFKA_SERVER],
        # Tự động đọc từ message mới nhất
        auto_offset_reset='earliest',
        # Chuyển đổi giá trị từ bytes về string
        
        value_deserializer=lambda m: m.decode('utf-8')
    )

    producer = create_kafka_producer()

    print(f"Đang lắng nghe các câu hỏi trên topic: '{QUESTION_TOPIC}'...")

    # --- Bước 5: Vòng lặp vô hạn để xử lý message ---
    for message in consumer:
        try:
            # message.value là một chuỗi JSON
            incoming_payload_str = message.value
            print(f"\nNhận được message: {incoming_payload_str}")

            # Chuyển chuỗi JSON về lại dictionary Python
            incoming_payload = json.loads(incoming_payload_str)

            question = incoming_payload.get('content')

            if not isinstance(question, str) or not question.strip():
                print("Lỗi: Message không hợp lệ, thiếu trường 'content'.")
                continue

            print(f"Đang xử lý câu hỏi: {question}")

            # Gọi RAG chain để lấy kết quả
            rag_output = rag_chain.invoke(question)

            # Xây dựng payload trả về
            response_payload = incoming_payload
            response_payload.pop('content', None)
            response_payload.update(rag_output)

            # Gửi kết quả vào topic 'answers_topic'
            print(f"Đang gửi câu trả lời đến topic '{ANSWER_TOPIC}'...")
            producer.send(ANSWER_TOPIC, value=response_payload)
            producer.flush()  # Đảm bảo message được gửi đi ngay lập tức
            print("Gửi câu trả lời thành công.")

        except json.JSONDecodeError:
            print(f"Lỗi: Không thể parse JSON từ message: {message.value}")
        except Exception as e:
            print(f"Đã có lỗi không xác định xảy ra khi xử lý message: {e}")


if __name__ == '__main__':
    main()