package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomParticipantJpaRepository extends JpaRepository<RoomParticipantJpaEntity, Long> {
}
