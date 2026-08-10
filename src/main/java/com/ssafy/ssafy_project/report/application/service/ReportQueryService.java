package com.ssafy.ssafy_project.report.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.report.application.port.in.GetReportPortIn;
import com.ssafy.ssafy_project.report.application.port.in.GetReportResult;
import com.ssafy.ssafy_project.report.application.port.out.ReportQueryPortOut;
import com.ssafy.ssafy_project.report.domain.Report;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService implements GetReportPortIn {

    private final LoadRoomPortOut loadRoomPortOut;
    private final ReportQueryPortOut reportQueryPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @Override
    public GetReportResult getReport(Long roomId, Long userId) {
        loadRoomPortOut.loadById(roomId);

        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        Report report = reportQueryPortOut.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.REPORT_NOT_FOUND));

        return new GetReportResult(
                report.getId(),
                report.getOwnerId(),
                report.getRoomId(),
                report.getContent(),
                report.getCreatedTime(),
                report.getTitle()
        );
    }
}
