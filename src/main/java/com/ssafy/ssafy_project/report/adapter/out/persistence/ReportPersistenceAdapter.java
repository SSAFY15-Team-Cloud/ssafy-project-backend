package com.ssafy.ssafy_project.report.adapter.out.persistence;

import com.ssafy.ssafy_project.report.application.port.out.ReportCommandPortOut;
import com.ssafy.ssafy_project.report.application.port.out.ReportQueryPortOut;
import com.ssafy.ssafy_project.report.domain.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements ReportCommandPortOut, ReportQueryPortOut {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public Report save(Report report) {
        ReportJpaEntity saved = reportJpaRepository.saveAndFlush(
                ReportJpaEntity.builder()
                        .id(report.getId())
                        .ownerId(report.getOwnerId())
                        .roomId(report.getRoomId())
                        .content(report.getContent())
                        .createdTime(report.getCreatedTime())
                        .title(report.getTitle())
                        .status(report.isStatus())
                        .build()
        );

        return Report.builder()
                .id(saved.getId())
                .ownerId(saved.getOwnerId())
                .roomId(saved.getRoomId())
                .content(saved.getContent())
                .createdTime(saved.getCreatedTime())
                .title(saved.getTitle())
                .status(saved.isStatus())
                .build();
    }

    @Override
    public boolean existsByRoomId(Long roomId) {
        return reportJpaRepository.existsByRoomId(roomId);
    }
}
