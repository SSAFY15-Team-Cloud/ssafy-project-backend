package com.ssafy.ssafy_project.report.application.port.out;

import com.ssafy.ssafy_project.report.domain.Report;

import java.util.Optional;

public interface ReportQueryPortOut {

    boolean existsByRoomId(Long roomId);

    Optional<Report> findByRoomId(Long roomId);
}
