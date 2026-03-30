package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantJpaRepository extends JpaRepository<RoomParticipantJpaEntity, Long> {
    Optional<RoomParticipantJpaEntity> findByRoomJpaEntity_IdAndUserJpaEntity_Id(Long roomId, Long userId);
    Optional<RoomParticipantJpaEntity> findById(Long roomParticipantId);

    List<RoomParticipantJpaEntity> findAllByRoomJpaEntity_IdAndIsActiveTrue(Long roomId);

    Optional<RoomParticipantJpaEntity> findByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(Long id, Long id1);
}
