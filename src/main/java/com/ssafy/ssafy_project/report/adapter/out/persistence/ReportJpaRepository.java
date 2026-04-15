package com.ssafy.ssafy_project.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    boolean existsByRoomId(Long roomId);
}
