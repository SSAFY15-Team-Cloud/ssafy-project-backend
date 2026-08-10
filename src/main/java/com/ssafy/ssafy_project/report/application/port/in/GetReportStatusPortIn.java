package com.ssafy.ssafy_project.report.application.port.in;

public interface GetReportStatusPortIn {

    GetReportStatusResult getStatus(Long roomId, Long userId);
}
