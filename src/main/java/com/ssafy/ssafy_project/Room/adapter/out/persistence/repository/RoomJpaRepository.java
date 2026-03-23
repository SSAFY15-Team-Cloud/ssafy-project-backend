package com.ssafy.ssafy_project.Room.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.Room.adapter.out.persistence.entity.RoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, Long> {
}
