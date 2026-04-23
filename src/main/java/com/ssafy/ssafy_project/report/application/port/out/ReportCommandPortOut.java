package com.ssafy.ssafy_project.report.application.port.out;

import com.ssafy.ssafy_project.report.domain.Report;

public interface ReportCommandPortOut {

    Report save(Report report);
}
