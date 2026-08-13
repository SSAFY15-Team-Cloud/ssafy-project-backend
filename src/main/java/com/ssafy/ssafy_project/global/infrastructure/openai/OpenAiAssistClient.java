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

    /** 트랜스크립트에서 액션아이템 JSON({"actionItems":[{"assignee","task","due"}]})을 추출한다. */
    public String extractActionItemsJson(String meetingTitle, String transcript) {
        String system = """
                You are extracting actionable tasks from a finished meeting transcript.
                Respond in Korean with a single JSON object:
                {"actionItems": [{"assignee": "담당자 이름 또는 미정", "task": "할 일 (60자 이내)", "due": "기한 또는 미정"}, ...]}
                Rules:
                - Include ONLY tasks actually supported by the transcript. Do not invent tasks.
                - Use the speaker names that appear in the transcript for assignee when clear.
                - Maximum 10 items. If there are none, return {"actionItems": []}.
                """;

        String user = """
                Meeting title: %s

                Transcript:
                %s
                """.formatted(meetingTitle, transcript);

        return complete(system, user, Map.of("type", "json_object"));
    }

    /** 번호 매긴 트랜스크립트에서 주제 전환점 챕터 JSON({"chapters":[{"title","segmentIndex"}]})을 추출한다. */
    public String generateChaptersJson(String meetingTitle, String numberedTranscript) {
        String system = """
                You are analyzing a finished meeting transcript to create podcast-style chapters.
                Each transcript line is numbered starting from 0.
                Identify 3 to 8 topic transitions and respond in Korean with a single JSON object:
                {"chapters": [{"title": "챕터 제목 (30자 이내)", "segmentIndex": 0}, ...]}
                Rules:
                - segmentIndex is the line number where the topic starts. The first chapter must start at 0.
                - segmentIndex values must be strictly increasing.
                - Titles must be concrete (무엇을 논의했는지), not generic like '도입부'.
                - If the transcript is too short for multiple topics, return a single chapter.
                """;

        String user = """
                Meeting title: %s

                Numbered transcript:
                %s
                """.formatted(meetingTitle, numberedTranscript);

        return complete(system, user, Map.of("type", "json_object"));
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
