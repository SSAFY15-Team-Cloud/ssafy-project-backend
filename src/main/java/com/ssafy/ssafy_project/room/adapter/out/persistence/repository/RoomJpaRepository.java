package com.ssafy.ssafy_project.room.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, Long> {
    Optional<RoomJpaEntity> findById(Long roomId);

    Optional<RoomJpaEntity> findByRoomCode(String roomCode);

    List<RoomJpaEntity> findAllByUserJpaEntity_IdAndStatus(Long userId, RoomStatus status);
}
