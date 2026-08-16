package com.chatboxai.chat_service.util.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Nhận câu trả lời do ai-service gửi về.
 *
 * ĐÃ SỬA so với bản gốc: bản gốc tiêm MessagesService rồi gọi
 * messagesService.processAiResponseMessage(data). Bên này chưa có phương thức đó,
 * nên lớp này tạm thời chỉ ghi log — lắp vào là chạy được ngay, không phải sửa
 * thêm file nào khác. Chỗ gọi service thật đánh dấu bằng TODO bên dưới.
 */
@Component
@RequiredArgsConstructor
public class KafkaMessageListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageListener.class);

    // TODO khi triển khai thật: tiêm ChatService (hoặc một ReplyHandler riêng) vào đây
    // private final ChatService chatService;

    @KafkaListener(
            topics = "${app.kafka.topic.answers}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(String data) {
        log.info("Nhận câu trả lời từ Kafka: {}", data);

        // TODO khi triển khai thật, thay dòng log trên bằng:
        //   1. bóc JSON ra đối tượng ReplyGeneratedEvent
        //   2. chatService.completeTurn(...) để lưu câu trả lời
        //   3. chống trùng: bản tin có thể đến hai lần, xem mục 5.3 bản kế hoạch
        //
        // Ném ngoại lệ ra khỏi phương thức này sẽ khiến Kafka giao lại bản tin,
        // và nếu lỗi lặp mãi thì phân vùng bị tắc. Phải có giới hạn số lần thử
        // lại kèm topic chứa bản tin chết — xem mục 5.4 bản kế hoạch.
    }
}
