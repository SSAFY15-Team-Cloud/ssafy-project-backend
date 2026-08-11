package com.ssafy.ssafy_project.global.infrastructure.openai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 회의 코파일럿(RAG Q&A)과 자막 번역용 범용 챗 클라이언트.
 */
@Component
@RequiredArgsConstructor
public class OpenAiAssistClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.openai.insight-model:gpt-4o-mini}")
    private String model;

    public String answerQuestion(String meetingTitle, String transcript, String wikiContext, String question) {
        String system = """
                You are a meeting copilot embedded in a live video meeting.
                Answer the user's question using ONLY the provided meeting transcript and wiki documents.
                If the answer is not in the provided context, say so honestly (in Korean).
                Respond in Korean. Be concise: 3-5 sentences or a short bullet list.
                When you use a wiki document, mention its title naturally.
                """;

        String user = """
                Meeting title: %s

                === Meeting transcript so far ===
                %s

                === Related wiki documents ===
                %s

                === Question ===
                %s
                """.formatted(meetingTitle, transcript, wikiContext.isBlank() ? "(없음)" : wikiContext, question);

        return complete(system, user, null);
    }

    /** 입력 순서를 유지한 번역 결과 JSON({"translations":[...]})을 반환한다. */
    public String translateJson(List<String> texts, String targetLanguage) {
        String system = """
                You are a subtitle translator for a live meeting.
                Translate each input line into %s.
                Keep names, numbers, and technical terms accurate. Keep the tone conversational.
                Respond with a single JSON object: {"translations": ["...", ...]}
                The array MUST have exactly the same number of items, in the same order as the input.
                """.formatted(targetLanguage);

        StringBuilder user = new StringBuilder("Input lines:\n");
        for (int i = 0; i < texts.size(); i++) {
            user.append(i + 1).append(". ").append(texts.get(i).replace("\n", " ")).append('\n');
        }

        return complete(system, user.toString(), Map.of("type", "json_object"));
    }

    private String complete(String systemPrompt, String userPrompt, Map<String, String> responseFormat) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("developer", systemPrompt),
                        new ChatMessage("user", userPrompt)
                ),
                0.2,
                responseFormat
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
            throw new IllegalStateException("OpenAI assist response is empty.");
        }

        return response.choices().get(0).message().content().trim();
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature,
            Map<String, String> response_format
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<ChatChoice> choices) {
    }

    private record ChatChoice(ChatResponseMessage message) {
    }

    private record ChatResponseMessage(String content) {
    }
}
