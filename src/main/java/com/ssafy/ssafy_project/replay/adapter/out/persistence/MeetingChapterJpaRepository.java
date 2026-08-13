package com.ssafy.ssafy_project.replay.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingChapterJpaRepository extends JpaRepository<MeetingChapterJpaEntity, Long> {

    Optional<MeetingChapterJpaEntity> findByRoomId(Long roomId);
}
