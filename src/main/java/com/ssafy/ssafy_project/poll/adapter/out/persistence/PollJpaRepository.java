package com.ssafy.ssafy_project.poll.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollJpaRepository extends JpaRepository<PollJpaEntity, Long> {

    List<PollJpaEntity> findAllByRoomIdOrderByIdDesc(Long roomId);
}
