package com.chatboxai.chat_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Khai báo topic để Kafka tự tạo lúc ứng dụng khởi động, khỏi phải tạo tay.
 *
 * ĐÃ SỬA so với bản gốc: bản gốc chỉ có một bean tên "topic1" do Spring Initializr
 * sinh ra và không dùng ở đâu. Nay khai báo đúng hai topic mà hệ thống thực sự dùng.
 *
 * partitions = 3 để nhiều worker chạy song song được; replicas = 1 vì chỉ có một
 * broker. Lên môi trường thật, replicas phải >= 3 thì mất một broker mới không mất dữ liệu.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String QUESTIONS_TOPIC = "questions_topic";
    public static final String ANSWERS_TOPIC = "answers_topic";

    @Bean
    public NewTopic questionsTopic() {
        return TopicBuilder.name(QUESTIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic answersTopic() {
        return TopicBuilder.name(ANSWERS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
