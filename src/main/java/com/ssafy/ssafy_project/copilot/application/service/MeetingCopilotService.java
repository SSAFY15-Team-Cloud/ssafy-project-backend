package com.ssafy.ssafy_project.copilot.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiAssistClient;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiEmbeddingClient;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeChunkJdbcAdapter;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 회의 코파일럿: 트랜스크립트 + 지식 위키를 컨텍스트로 참가자의 질문에 답한다 (RAG).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingCopilotService {

    private static final int MAX_TRANSCRIPT_CHARS = 10_000;
    private static final int WIKI_TOP_K = 4;
    private static final double MIN_SCORE = 0.2;

    private final AudioTextJpaRepository audioTextJpaRepository;
    private final LoadRoomPortOut loadRoomPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final OpenAiEmbeddingClient embeddingClient;
    private final KnowledgeChunkJdbcAdapter chunkAdapter;
    private final OpenAiAssistClient assistClient;

    public CopilotAnswer ask(Long roomId, Long userId, String question) {
        return ask(roomId, userId, question, List.of());
    }

    public CopilotAnswer ask(Long roomId, Long userId, String question, List<HistoryTurn> history) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        Room room = loadRoomPortOut.loadById(roomId);

        StringBuilder transcriptBuilder = new StringBuilder();
        audioTextJpaRepository.findTranscriptSegmentsByRoomIdOrderByAudioTime(roomId).forEach(segment -> {
            if (segment.getText() != null && !segment.getText().isBlank()) {
                transcriptBuilder.append(segment.getSpeakerName())
                        .append(": ")
                        .append(segment.getText().trim())
                        .append('\n');
            }
        });
        String transcript = transcriptBuilder.toString();
        if (transcript.isBlank()) {
            transcript = "(아직 발화 기록이 없습니다)";
        } else if (transcript.length() > MAX_TRANSCRIPT_CHARS) {
            transcript = transcript.substring(transcript.length() - MAX_TRANSCRIPT_CHARS);
        }

        // 질문과 관련된 위키 문서 검색
        List<CopilotSource> sources = new ArrayList<>();
        StringBuilder wikiContext = new StringBuilder();
        try {
            float[] queryEmbedding = embeddingClient.embedOne(question);
            Set<Long> seenDocuments = new HashSet<>();
            for (KnowledgeChunkJdbcAdapter.ChunkMatch match : chunkAdapter.searchSimilar(queryEmbedding, WIKI_TOP_K * 2)) {
                if (match.score() < MIN_SCORE || !seenDocuments.add(match.documentId())) {
                    continue;
                }
                wikiContext.append("[").append(match.title()).append("]\n")
                        .append(match.content()).append("\n\n");
                sources.add(new CopilotSource(
                        match.documentId(),
                        match.title(),
                        Math.round(match.score() * 1000d) / 1000d
                ));
                if (sources.size() >= WIKI_TOP_K) {
                    break;
                }
            }
        } catch (Exception ignored) {
            // 위키 검색 실패는 답변 자체를 막지 않는다 (트랜스크립트만으로 답변)
        }

        // 후속 질문 컨텍스트 (최근 5턴, null/과대 길이 방어)
        StringBuilder historyText = new StringBuilder();
        if (history != null) {
            history.stream()
                    .skip(Math.max(0, history.size() - 5))
                    .filter(turn -> turn != null
                            && turn.question() != null && !turn.question().isBlank()
                            && turn.answer() != null && !turn.answer().isBlank())
                    .forEach(turn -> historyText
                            .append("Q: ").append(truncate(turn.question(), 500)).append('\n')
                            .append("A: ").append(truncate(turn.answer(), 2000)).append("\n\n"));
        }

        String answer = assistClient.answerQuestion(
                room.getTitle(), transcript, wikiContext.toString(), historyText.toString(), question);
        return new CopilotAnswer(answer, sources);
    }

    public record HistoryTurn(String question, String answer) {
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    public record CopilotAnswer(String answer, List<CopilotSource> sources) {
    }

    public record CopilotSource(Long documentId, String title, double score) {
    }
}
