package com.ssafy.ssafy_project.audio.adapter.out.messaging;

import com.ssafy.ssafy_project.audio.application.port.out.AudioSttMessagePortOut;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAudioSttMessageProducer implements AudioSttMessagePortOut {

    @Override
    public void send(Long audioId, Long roomId) {
        log.info("RabbitMQ is disabled. Skipping STT message publish. audioId={}, roomId={}", audioId, roomId);
    }
}
