package com.ssafy.ssafy_project.global.infrastructure.openai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 회의 진행 중 롤링 인사이트(요약/액션아이템/논점)를 JSON으로 추출한다.
 */
@Component
@RequiredArgsConstructor
public class OpenAiInsightClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.openai.insight-model:gpt-4o-mini}")
    private String model;

    public String extractInsightJson(String meetingTitle, String transcript) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("developer", DEVELOPER_PROMPT),
                        new ChatMessage("user", buildUserPrompt(meetingTitle, transcript))
                ),
                0.2,
                Map.of("type", "json_object")
        );

        ChatCompletionResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null) {
            throw new IllegalStateException("OpenAI insight response is empty.");
        }

        return response.choices().get(0).message().content().trim();
    }

    private static final String DEVELOPER_PROMPT = """
            You are a live meeting assistant analyzing an in-progress meeting transcript.
            Use only the provided transcript as evidence. Do not invent facts.
            Respond in Korean, as a single JSON object with exactly these keys:
            {
              "summary": "지금까지의 회의 흐름을 3문장 이내로 요약",
              "keyPoints": ["핵심 논점 (최대 5개)"],
              "actionItems": [{"assignee": "담당자 또는 미정", "task": "할 일", "due": "기한 또는 미정"}],
              "openQuestions": ["아직 결론이 나지 않은 질문 (최대 3개)"],
              "mood": {"emoji": "회의 분위기를 나타내는 이모지 1개", "label": "분위기 한 단어 (예: 활발함, 차분함, 긴장됨)"}
            }
            Rules:
            - Arrays may be empty if nothing reliable exists.
            - Keep each item under 80 characters.
            - Reflect the most recent discussion with higher weight.
            """;

    private String buildUserPrompt(String meetingTitle, String transcript) {
        return """
                Meeting title: %s

                Transcript so far:
                %s
                """.formatted(meetingTitle, transcript);
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature,
            Map<String, String> response_format
    ) {
    }

    private record ChatMessage(
            String role,
            String content
    ) {
    }

    private record ChatCompletionResponse(
            List<ChatChoice> choices
    ) {
    }

    private record ChatChoice(
            ChatResponseMessage message
    ) {
    }

    private record ChatResponseMessage(
            String content
    ) {
    }
}
