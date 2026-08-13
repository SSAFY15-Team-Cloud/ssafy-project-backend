package com.ssafy.ssafy_project.search.application.service;

import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiEmbeddingClient;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeChunkJdbcAdapter;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;
import com.ssafy.ssafy_project.search.adapter.out.persistence.TranscriptChunkJdbcAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 통합 시맨틱 검색: 지식 위키 + (내가 참여한 회의의) 발화 기록을 한 번에 검색한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnifiedSearchService {

    private static final double MIN_SCORE = 0.15;
    private static final int WIKI_LIMIT = 5;
    private static final int MEETING_LIMIT = 8;

    private final OpenAiEmbeddingClient embeddingClient;
    private final KnowledgeChunkJdbcAdapter knowledgeChunkAdapter;
    private final TranscriptChunkJdbcAdapter transcriptChunkAdapter;
    private final RoomParticipantJpaRepository roomParticipantJpaRepository;

    public SearchResult search(Long userId, String query) {
        float[] queryEmbedding = embeddingClient.embedOne(query);

        // 위키 문서 (문서 단위 중복 제거)
        List<WikiHit> wikiHits = new ArrayList<>();
        Set<Long> seenDocuments = new HashSet<>();
        for (KnowledgeChunkJdbcAdapter.ChunkMatch match :
                knowledgeChunkAdapter.searchSimilar(queryEmbedding, WIKI_LIMIT * 3)) {
            if (match.score() < MIN_SCORE || !seenDocuments.add(match.documentId())) {
                continue;
            }
            wikiHits.add(new WikiHit(
                    match.documentId(),
                    match.title(),
                    match.filename(),
                    snippet(match.content()),
                    round(match.score())
            ));
            if (wikiHits.size() >= WIKI_LIMIT) {
                break;
            }
        }

        // 회의 발화 — 내가 참여했던 방만 (인가 경계)
        List<Long> myRoomIds = roomParticipantJpaRepository.findAllWithRoomByUserId(userId).stream()
                .map(p -> p.getRoomJpaEntity().getId())
                .distinct()
                .toList();

        List<MeetingHit> meetingHits = transcriptChunkAdapter
                .searchSimilarInRooms(queryEmbedding, myRoomIds, MEETING_LIMIT)
                .stream()
                .filter(match -> match.score() >= MIN_SCORE)
                .map(match -> new MeetingHit(
                        match.roomId(),
                        match.roomTitle(),
                        match.speakerName(),
                        snippet(match.content()),
                        match.spokeTime(),
                        round(match.score())
                ))
                .toList();

        return new SearchResult(wikiHits, meetingHits);
    }

    private String snippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 220 ? normalized.substring(0, 220) + "…" : normalized;
    }

    private double round(double score) {
        return Math.round(score * 1000d) / 1000d;
    }

    public record SearchResult(List<WikiHit> wiki, List<MeetingHit> meetings) {
    }

    public record WikiHit(
            Long documentId,
            String title,
            String filename,
            String snippet,
            double score
    ) {
    }

    public record MeetingHit(
            Long roomId,
            String roomTitle,
            String speakerName,
            String snippet,
            LocalDateTime spokeTime,
            double score
    ) {
    }
}
