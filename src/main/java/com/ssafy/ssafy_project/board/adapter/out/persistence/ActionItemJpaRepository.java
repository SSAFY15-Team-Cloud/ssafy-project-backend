package com.ssafy.ssafy_project.board.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionItemJpaRepository extends JpaRepository<ActionItemJpaEntity, Long> {

    List<ActionItemJpaEntity> findAllByRoomIdOrderByIdAsc(Long roomId);

    boolean existsByRoomId(Long roomId);
}
