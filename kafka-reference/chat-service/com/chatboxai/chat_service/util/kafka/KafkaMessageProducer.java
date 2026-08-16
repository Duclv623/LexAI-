package com.chatboxai.chat_service.util.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Bọc KafkaTemplate cho gọn.
 *
 * ĐÃ THÊM so với bản gốc: phương thức send(topic, key, message). Bản gốc chỉ có
 * bản không khoá, nghĩa là Kafka rải bản tin đều ra mọi phân vùng và không còn
 * bảo đảm thứ tự. Truyền conversationId làm khoá thì mọi tin nhắn của cùng một
 * hội thoại luôn rơi vào một phân vùng, nhờ đó được xử lý tuần tự.
 */
@Component
@RequiredArgsConstructor
public class KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    /** Không khoá: Kafka tự chọn phân vùng, KHÔNG bảo đảm thứ tự. */
    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    /** Có khoá: cùng khoá thì cùng phân vùng, nhờ đó giữ được thứ tự. */
    public void sendMessage(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // gửi thất bại thì bản tin mất luôn, phải ghi log để còn biết mà xử lý
                        log.error("Gửi bản tin lên topic {} thất bại (key={})", topic, key, ex);
                    } else {
                        log.debug("Đã gửi lên {} phân vùng {} vị trí {}", topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
