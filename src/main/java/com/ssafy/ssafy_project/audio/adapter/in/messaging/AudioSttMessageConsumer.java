package com.ssafy.ssafy_project.audio.adapter.in.messaging;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.AudioSttRequestMessage;
import com.ssafy.ssafy_project.audio.application.service.AudioSttWorkerService;
import com.ssafy.ssafy_project.global.infrastructure.config.AudioSttRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class AudioSttMessageConsumer {

    private final AudioSttWorkerService audioSttWorkerService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = AudioSttRabbitConfig.AUDIO_STT_QUEUE)
    public void consume(Message message) {
        AudioSttRequestMessage requestMessage = readMessage(message);
        log.info("STT message received. audioId={}, roomId={}", requestMessage.audioId(), requestMessage.roomId());
        audioSttWorkerService.processSingleAudioById(requestMessage.audioId());
    }

    private AudioSttRequestMessage readMessage(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), AudioSttRequestMessage.class);
        } catch (Exception e) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            throw new RuntimeException("Failed to deserialize STT message. body=" + body, e);
        }
    }
}
