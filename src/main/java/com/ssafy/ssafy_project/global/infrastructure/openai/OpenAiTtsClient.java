package com.ssafy.ssafy_project.global.infrastructure.openai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiTtsClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.openai.tts-model:tts-1}")
    private String model;

    @Value("${app.openai.tts-voice:nova}")
    private String voice;

    public byte[] synthesize(String text) {
        byte[] audio = restClient.post()
                .uri("/audio/speech")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "voice", voice,
                        "input", text,
                        "response_format", "mp3"
                ))
                .retrieve()
                .body(byte[].class);

        if (audio == null || audio.length == 0) {
            throw new IllegalStateException("OpenAI TTS response is empty.");
        }
        return audio;
    }
}
