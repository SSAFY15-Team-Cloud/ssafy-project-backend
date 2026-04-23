package com.ssafy.ssafy_project.audio.adapter.out.messaging;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.AudioSttRequestMessage;
import com.ssafy.ssafy_project.audio.application.port.out.AudioSttMessagePortOut;
import com.ssafy.ssafy_project.global.infrastructure.config.AudioSttRabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class AudioSttMessageProducer implements AudioSttMessagePortOut {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(Long audioId, Long roomId) {
        try {
            AudioSttRequestMessage payload = new AudioSttRequestMessage(audioId, roomId);
            byte[] body = objectMapper.writeValueAsBytes(payload);
            Message message = MessageBuilder.withBody(body)
                    .setContentType("application/json")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            rabbitTemplate.send(
                    AudioSttRabbitConfig.AUDIO_EXCHANGE,
                    AudioSttRabbitConfig.AUDIO_STT_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish audio STT message. audioId=" + audioId, e);
        }
    }
}
