package com.ssafy.ssafy_project.report.application.port.in;

public interface GetReportPortIn {

    GetReportResult getReport(Long roomId, Long userId);
}
