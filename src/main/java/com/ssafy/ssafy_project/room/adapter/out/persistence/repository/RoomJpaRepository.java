package com.ssafy.ssafy_project.room.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, Long> {
    Optional<RoomJpaEntity> findById(Long roomId);

    Optional<RoomJpaEntity> findByRoomCode(String roomCode);
}
