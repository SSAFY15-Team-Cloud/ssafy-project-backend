package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantJpaRepository extends JpaRepository<RoomParticipantJpaEntity, Long> {
    Optional<RoomParticipantJpaEntity> findByRoomJpaEntity_IdAndUserJpaEntity_Id(Long roomId, Long userId);

    @Query("""
            SELECT rp FROM RoomParticipantJpaEntity rp
            JOIN FETCH rp.userJpaEntity
            WHERE rp.roomJpaEntity.id = :roomId AND rp.isActive = true
            """)
    List<RoomParticipantJpaEntity> findAllByRoomJpaEntity_IdAndIsActiveTrue(@Param("roomId")Long roomId);

    Optional<RoomParticipantJpaEntity> findByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(Long id, Long id1);

    boolean existsByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(Long roomId, Long userId);

    boolean existsByRoomJpaEntity_IdAndUserJpaEntity_Id(Long roomId, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
          UPDATE room_participants
          SET duration_time = duration_time + EXTRACT(EPOCH FROM (:now - joined_time)) * 1000::bigint,
              is_active = false
          WHERE room_id = :roomId
            AND is_active = true
          """, nativeQuery = true)
    void closeActiveParticipantsByRoomId(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE room_participants
             SET duration_time = duration_time + EXTRACT(EPOCH FROM (:now - joined_time)) * 1000::bigint,
                  is_active = false
                WHERE user_id = :userId
             AND is_active = true
     """,nativeQuery = true)
    void closeActiveParticipantsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<RoomParticipantJpaEntity> findAllByUserJpaEntity_IdAndIsActiveTrue(Long userId);

}
