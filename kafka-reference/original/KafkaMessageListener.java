package com.jurisai.util.kafka;

import com.jurisai.service.MessagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMessageListener {
    private final MessagesService messagesService;

    @KafkaListener(
            topics = "${kafka.topic.answers_topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(String data) {
        messagesService.processAiResponseMessage(data);
    }
}
