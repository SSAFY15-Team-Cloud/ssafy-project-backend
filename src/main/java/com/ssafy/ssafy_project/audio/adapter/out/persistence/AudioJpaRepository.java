package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AudioJpaRepository extends JpaRepository<AudioJpaEntity, Long> {

    interface SpeakerDurationProjection {
        Long getSpeakerId();

        Long getTotalDuration();
    }

    @Query("""
            SELECT a.speakerId AS speakerId, SUM(COALESCE(a.duration, 0)) AS totalDuration
            FROM AudioJpaEntity a
            WHERE a.roomId = :roomId
            GROUP BY a.speakerId
            ORDER BY SUM(COALESCE(a.duration, 0)) DESC
            """)
    List<SpeakerDurationProjection> sumDurationBySpeaker(@Param("roomId") Long roomId);

    Optional<AudioJpaEntity> findById(Long id);

    List<AudioJpaEntity> findAllByRoomId(Long roomId);

    List<AudioJpaEntity> findAllByRoomIdAndSttStatus(Long roomId, AudioSttStatus sttStatus);

    boolean existsByRoomIdAndSttStatusIn(Long roomId, Set<AudioSttStatus> sttStatuses);
}
