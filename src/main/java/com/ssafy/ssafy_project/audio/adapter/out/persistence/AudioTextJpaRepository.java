package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AudioTextJpaRepository extends JpaRepository<AudioTextJpaEntity, Long> {

    interface TranscriptSegmentProjection {
        String getSpeakerName();

        LocalDateTime getStartTime();

        LocalDateTime getEndTime();

        String getText();
    }

    @Query(
            value = "SELECT COALESCE(u.nickname, u.name, 'speaker-' || CAST(a.speaker_id AS VARCHAR)) AS \"speakerName\", " +
                    "a.start_time AS \"startTime\", " +
                    "a.end_time AS \"endTime\", " +
                    "t.text AS \"text\" " +
                    "FROM audio_text t " +
                    "JOIN audio a ON a.id = t.audio_id " +
                    "LEFT JOIN app_user u ON u.id = a.speaker_id " +
                    "WHERE a.room_id = :roomId " +
                    "ORDER BY a.start_time ASC NULLS LAST, a.created_time ASC, t.created_time ASC",
            nativeQuery = true
    )
    List<TranscriptSegmentProjection> findTranscriptSegmentsByRoomIdOrderByAudioTime(@Param("roomId") Long roomId);
}
