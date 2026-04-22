package com.ssafy.ssafy_project.global.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class AudioSttRabbitConfig {

    public static final String AUDIO_EXCHANGE = "audio.exchange";
    public static final String AUDIO_STT_QUEUE = "audio.stt.queue";
    public static final String AUDIO_STT_ROUTING_KEY = "audio.stt.request";

    @Bean
    public TopicExchange audioExchange() {
        return new TopicExchange(AUDIO_EXCHANGE);
    }

    @Bean
    public Queue audioSttQueue() {
        return QueueBuilder.durable(AUDIO_STT_QUEUE).build();
    }

    @Bean
    public Binding audioSttBinding() {
        return BindingBuilder.bind(audioSttQueue())
                .to(audioExchange())
                .with(AUDIO_STT_ROUTING_KEY);
    }
}
