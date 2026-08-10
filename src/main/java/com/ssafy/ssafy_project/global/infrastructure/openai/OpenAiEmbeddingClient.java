package com.ssafy.ssafy_project.global.infrastructure.openai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.openai.embedding-model:text-embedding-3-small}")
    private String model;

    public List<float[]> embed(List<String> inputs) {
        EmbeddingRequest request = new EmbeddingRequest(model, inputs);

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().size() != inputs.size()) {
            throw new IllegalStateException("OpenAI embedding response is invalid.");
        }

        return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(data -> {
                    List<Double> values = data.embedding();
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = values.get(i).floatValue();
                    }
                    return vector;
                })
                .toList();
    }

    public float[] embedOne(String input) {
        return embed(List.of(input)).getFirst();
    }

    private record EmbeddingRequest(
            String model,
            List<String> input
    ) {
    }

    private record EmbeddingResponse(
            List<EmbeddingData> data
    ) {
    }

    private record EmbeddingData(
            Integer index,
            List<Double> embedding
    ) {
    }
}
