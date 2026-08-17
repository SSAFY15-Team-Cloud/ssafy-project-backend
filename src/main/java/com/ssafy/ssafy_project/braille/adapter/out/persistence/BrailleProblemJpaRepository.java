package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrailleProblemJpaRepository extends JpaRepository<BrailleProblemJpaEntity, Long> {

    List<BrailleProblemJpaEntity> findAllByRoomIdOrderByIdDesc(Long roomId);

    List<BrailleProblemJpaEntity> findTop30ByRoomIdIsNullAndCreatorIdOrderByIdDesc(Long creatorId);
}
