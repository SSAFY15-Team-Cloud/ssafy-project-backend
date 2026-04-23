package com.ssafy.ssafy_project.global.infrastructure.openai;

import com.ssafy.ssafy_project.audio.application.port.out.TranscriptSegment;
import com.ssafy.ssafy_project.report.application.port.out.MeetingSummaryPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OpenAiMeetingSummaryClient implements MeetingSummaryPortOut {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${app.openai.report-summary-model:gpt-4o-mini}")
    private String model;

    @Override
    public String summarizeMeeting(String meetingTitle, List<TranscriptSegment> transcriptSegments) {
        String transcript = transcriptSegments.stream()
                .map(this::formatSegment)
                .collect(Collectors.joining(System.lineSeparator()));

        if (transcript.isBlank()) {
            throw new IllegalArgumentException("Transcript segments must not be empty.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("developer", buildDeveloperPrompt()),
                        new ChatMessage("user", buildUserPrompt(meetingTitle, transcript))
                ),
                0.2
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
                || response.choices().get(0).message().content() == null
                || response.choices().get(0).message().content().isBlank()) {
            throw new IllegalStateException("OpenAI meeting summary response is empty.");
        }

        return response.choices().get(0).message().content().trim();
    }

    private String formatSegment(TranscriptSegment segment) {
        return "[" + formatTime(segment.startTime()) + " ~ " + formatTime(segment.endTime()) + "] "
                + segment.speakerName() + ": " + segment.text().trim();
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "unknown";
        }

        return dateTime.format(TIME_FORMATTER);
    }

    private String buildDeveloperPrompt() {
        return """
                You are a meeting note assistant.
                Use only the provided transcript as evidence.
                Do not invent missing facts.
                If something is ambiguous, write '확인 필요'.
                Respond in Korean.
                Keep the exact section order below.

                [회의명]
                [회의 주제]
                [전체 요약]
                [발언자별 요약]
                [주요 결정사항]
                [액션 아이템]
                [미해결 쟁점]

                Writing rules:
                - Write concise but useful meeting minutes.
                - If a section has no reliable content, write '없음' or '확인 필요'.
                - In [발언자별 요약], organize key opinions, requests, and updates by speaker.
                - In [주요 결정사항], include only decisions actually supported by the transcript.
                - In [액션 아이템], use bullet lines in the format: 담당자 | 할 일 | 기한.
                - If a due date is not stated, write '미정'.
                - In [미해결 쟁점], list unresolved questions, disagreements, or follow-up checks.
                - Produce a polished summary, not a raw transcript dump.
                """;
    }

    private String buildUserPrompt(String meetingTitle, String transcript) {
        return """
                The following text is an STT transcript of a meeting.

                Meeting title:
                %s

                Transcript:
                %s
                """.formatted(meetingTitle, transcript);
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature
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
