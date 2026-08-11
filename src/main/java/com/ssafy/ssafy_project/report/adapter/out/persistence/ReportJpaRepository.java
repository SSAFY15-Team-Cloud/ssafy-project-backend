package com.ssafy.ssafy_project.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    boolean existsByRoomId(Long roomId);

    Optional<ReportJpaEntity> findByRoomId(Long roomId);

    List<ReportJpaEntity> findAllByRoomIdIn(Collection<Long> roomIds);
}
