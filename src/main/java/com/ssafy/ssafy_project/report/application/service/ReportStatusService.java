package com.ssafy.ssafy_project.report.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusPortIn;
import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusResult;
import com.ssafy.ssafy_project.report.application.port.out.ReportQueryPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportStatusService implements GetReportStatusPortIn {

    private final ReportQueryPortOut reportQueryPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @Override
    public GetReportStatusResult getStatus(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        return reportQueryPortOut.findByRoomId(roomId)
                .map(report -> new GetReportStatusResult(report.isStatus() ? "DONE" : "PENDING"))
                .orElseGet(() -> new GetReportStatusResult("PENDING"));
    }
}
