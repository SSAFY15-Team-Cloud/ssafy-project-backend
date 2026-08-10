package com.ssafy.ssafy_project.global.infrastructure.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqCommonConfig {

    // ObjectMapper(Jackson 3)는 Boot 자동구성 빈을 사용한다.
    // 여기서 중복 정의하면 테스트의 Jackson 2 objectMapper 빈과 이름이 충돌한다.

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}
