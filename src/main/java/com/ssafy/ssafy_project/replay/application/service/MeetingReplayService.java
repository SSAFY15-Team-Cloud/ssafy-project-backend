package com.ssafy.ssafy_project.replay.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiAssistClient;
import com.ssafy.ssafy_project.global.infrastructure.s3.S3Properties;
import com.ssafy.ssafy_project.replay.adapter.out.persistence.MeetingChapterJpaEntity;
import com.ssafy.ssafy_project.replay.adapter.out.persistence.MeetingChapterJpaRepository;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회의 다시듣기: STT용으로 이미 저장된 발화 오디오 청크를 시간순으로 재조립한다.
 * 별도 녹화 없이(추가 저장 공간 0) 팟캐스트식 재생 + AI 챕터를 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingReplayService {

    private static final Duration REPLAY_URL_TTL = Duration.ofHours(2);
    private static final int MAX_CHAPTER_TRANSCRIPT_CHARS = 12_000;

    private final JdbcTemplate jdbcTemplate;
    private final LoadRoomPortOut loadRoomPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final MeetingChapterJpaRepository chapterRepository;
    private final OpenAiAssistClient assistClient;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReplayResult getReplay(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
        Room room = loadRoomPortOut.loadById(roomId);

        List<SegmentRow> rows = loadSegments(roomId);
        List<ReplaySegment> segments = new ArrayList<>();
        for (SegmentRow row : rows) {
            segments.add(new ReplaySegment(
                    row.audioId(),
                    row.speakerId(),
                    row.speakerName(),
                    row.text(),
                    row.mimeType(),
                    row.duration() != null ? row.duration() : 15,
                    row.startTime(),
                    presignGet(row.path())
            ));
        }

        List<Chapter> chapters = room.getStatus() == RoomStatus.ENDED
                ? loadOrGenerateChapters(room, rows)
                : List.of();

        return new ReplayResult(room.getTitle(), room.getStatus().name(), segments, chapters);
    }

    private List<SegmentRow> loadSegments(Long roomId) {
        return jdbcTemplate.query(
                """
                        SELECT a.id AS audio_id, a.speaker_id, a.path, a.mime_type, a.duration,
                               a.start_time,
                               COALESCE(u.nickname, u.name, 'speaker-' || CAST(a.speaker_id AS VARCHAR)) AS speaker_name,
                               t.text
                        FROM audio a
                        LEFT JOIN audio_text t ON t.audio_id = a.id
                        LEFT JOIN app_user u ON u.id = a.speaker_id
                        WHERE a.room_id = ? AND a.upload_status = 'UPLOADED'
                        ORDER BY a.start_time ASC NULLS LAST, a.created_time ASC
                        """,
                (rs, rowNum) -> new SegmentRow(
                        rs.getLong("audio_id"),
                        rs.getLong("speaker_id"),
                        rs.getString("speaker_name"),
                        rs.getString("text"),
                        rs.getString("path"),
                        rs.getString("mime_type"),
                        rs.getObject("duration") != null ? rs.getInt("duration") : null,
                        rs.getTimestamp("start_time") != null
                                ? rs.getTimestamp("start_time").toLocalDateTime() : null
                ),
                roomId
        );
    }

    private List<Chapter> loadOrGenerateChapters(Room room, List<SegmentRow> rows) {
        MeetingChapterJpaEntity cached = chapterRepository.findByRoomId(room.getId()).orElse(null);
        if (cached != null) {
            return parseChapters(cached.getChaptersJson(), rows.size());
        }

        List<SegmentRow> spoken = rows.stream()
                .filter(row -> row.text() != null && !row.text().isBlank())
                .toList();
        if (spoken.isEmpty()) {
            return List.of();
        }

        try {
            StringBuilder numbered = new StringBuilder();
            for (int i = 0; i < rows.size(); i++) {
                SegmentRow row = rows.get(i);
                if (row.text() == null || row.text().isBlank()) {
                    continue;
                }
                numbered.append(i).append(". ").append(row.speakerName()).append(": ")
                        .append(row.text().replaceAll("\\s+", " ").trim()).append('\n');
                if (numbered.length() > MAX_CHAPTER_TRANSCRIPT_CHARS) {
                    break;
                }
            }

            String json = assistClient.generateChaptersJson(room.getTitle(), numbered.toString());
            try {
                chapterRepository.save(new MeetingChapterJpaEntity(room.getId(), json));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 생성 — 저장된 쪽을 쓰면 된다
            }
            return parseChapters(json, rows.size());
        } catch (Exception e) {
            log.error("Chapter generation failed. roomId={}", room.getId(), e);
            return List.of();
        }
    }

    private List<Chapter> parseChapters(String json, int segmentCount) {
        try {
            JsonNode chaptersNode = objectMapper.readTree(json).path("chapters");
            List<Chapter> chapters = new ArrayList<>();
            for (JsonNode node : chaptersNode) {
                int index = node.path("segmentIndex").asInt(-1);
                String title = node.path("title").asString("");
                if (index >= 0 && index < segmentCount && !title.isBlank()) {
                    chapters.add(new Chapter(title, index));
                }
            }
            return chapters;
        } catch (Exception e) {
            log.warn("Failed to parse chapters json.", e);
            return List.of();
        }
    }

    // virtual-hosted / path-style 저장 경로 모두에서 오브젝트 키를 추출해 presigned GET 발급
    private String presignGet(String fullPath) {
        String key = URI.create(fullPath).getPath();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        String bucketSegment = s3Properties.s3().bucket() + "/";
        if (key.startsWith(bucketSegment)) {
            key = key.substring(bucketSegment.length());
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(key)
                .build();

        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(REPLAY_URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build()
        ).url().toString();
    }

    private record SegmentRow(
            Long audioId,
            Long speakerId,
            String speakerName,
            String text,
            String path,
            String mimeType,
            Integer duration,
            LocalDateTime startTime
    ) {
    }

    public record ReplayResult(
            String roomTitle,
            String roomStatus,
            List<ReplaySegment> segments,
            List<Chapter> chapters
    ) {
    }

    public record ReplaySegment(
            Long audioId,
            Long speakerId,
            String speakerName,
            String text,
            String mimeType,
            int duration,
            LocalDateTime startTime,
            String url
    ) {
    }

    public record Chapter(String title, int segmentIndex) {
    }
}
