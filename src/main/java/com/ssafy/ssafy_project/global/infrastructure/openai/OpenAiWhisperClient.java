package com.ssafy.ssafy_project.global.infrastructure.openai;

import com.ssafy.ssafy_project.audio.application.port.out.AudioTranscriptionPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAiWhisperClient implements AudioTranscriptionPortOut {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Override
    public String transcribe(byte[] audioBytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("model", "whisper-1");

        OpenAiTranscriptionResponse response = restClient.post()
                .uri("/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(OpenAiTranscriptionResponse.class);

        if (response == null || response.text() == null) {
            throw new IllegalStateException("OpenAI transcription response is empty.");
        }

        return response.text();
    }
}
