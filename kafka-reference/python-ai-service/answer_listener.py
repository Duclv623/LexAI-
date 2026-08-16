# File: answer_listener.py

import json
from kafka import KafkaConsumer

# --- Cấu hình Kafka ---
from config.config import KAFKA_SERVER, ANSWER_TOPIC


def listen_for_answers():
    """
    Hàm này kết nối đến Kafka và lắng nghe liên tục các câu trả lời
    trên topic 'answers_topic'.
    """
    print(f"--- Bắt đầu lắng nghe các câu trả lời trên topic '{ANSWER_TOPIC}' ---")
    print("Nhấn Ctrl+C để thoát.")

    # Khởi tạo một Consumer mới, chuyên cho việc đọc câu trả lời
    consumer = KafkaConsumer(
        ANSWER_TOPIC,
        bootstrap_servers=[KAFKA_SERVER],
        # Bắt đầu đọc từ các message cũ nhất chưa được đọc
        auto_offset_reset='earliest',
        # Chuyển đổi giá trị từ bytes về lại dictionary Python
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )

    try:
        # Vòng lặp vô hạn để in ra các message nhận được
        for message in consumer:
            # message.value bây giờ đã là một dictionary Python
            answer_payload = message.value

            print("\n" + "=" * 50)
            print(">>> ĐÃ NHẬN ĐƯỢC MỘT CÂU TRẢ LỜI MỚI <<<")
            print(f"    - Từ message_id: {answer_payload.get('message_id')}")
            print(f"    - Cho account_id: {answer_payload.get('account_id')}")
            print("-" * 20)
            print(f"    - Câu trả lời của AI:\n      {answer_payload.get('answer')}")

            # In ra các trích dẫn nếu có
            if 'citations' in answer_payload and answer_payload['citations']:
                print("\n    - Các trích dẫn:")
                for i, citation in enumerate(answer_payload['citations']):
                    print(f"      {i + 1}. Tên văn bản: {citation.get('law_name')}")
                    print(f"         (Điều: {citation.get('article')}, Chương: {citation.get('chapter')})")

            print("=" * 50 + "\n")

    except KeyboardInterrupt:
        print("\n--- Đã dừng lắng nghe. ---")
    finally:
        consumer.close()


if __name__ == '__main__':
    listen_for_answers()