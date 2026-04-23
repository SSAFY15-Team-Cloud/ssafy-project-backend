package com.ssafy.ssafy_project.report.application.service;

import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusPortIn;
import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusResult;
import com.ssafy.ssafy_project.report.application.port.out.ReportQueryPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportStatusService implements GetReportStatusPortIn {

    private final ReportQueryPortOut reportQueryPortOut;

    @Override
    public GetReportStatusResult getStatus(Long roomId) {
        return reportQueryPortOut.findByRoomId(roomId)
                .map(report -> new GetReportStatusResult(report.isStatus() ? "DONE" : "PENDING"))
                .orElseGet(() -> new GetReportStatusResult("PENDING"));
    }
}
