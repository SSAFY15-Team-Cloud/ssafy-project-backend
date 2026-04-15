package com.ssafy.ssafy_project.report.application.service;

import com.ssafy.ssafy_project.audio.application.port.out.TranscriptSegment;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.report.application.port.out.MeetingSummaryPortOut;
import com.ssafy.ssafy_project.report.application.port.out.ReportCommandPortOut;
import com.ssafy.ssafy_project.report.application.port.out.ReportQueryPortOut;
import com.ssafy.ssafy_project.report.domain.Report;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final String EMPTY_TRANSCRIPT_CONTENT = "No transcript text was recorded for this meeting.";

    private final LoadRoomPortOut loadRoomPortOut;
    private final AudioQueryPort audioQueryPort;
    private final MeetingSummaryPortOut meetingSummaryPortOut;
    private final ReportQueryPortOut reportQueryPortOut;
    private final ReportCommandPortOut reportCommandPortOut;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateIfReady(Long roomId) {
        if (reportQueryPortOut.existsByRoomId(roomId)) {
            log.debug("Report already exists. roomId={}", roomId);
            return;
        }

        Room room = loadRoomPortOut.loadById(roomId);
        if (room.getStatus() != RoomStatus.ENDED) {
            log.debug("Room is not ended yet. roomId={}, status={}", roomId, room.getStatus());
            return;
        }

        if (audioQueryPort.existsUnfinishedByRoomId(roomId)) {
            log.debug("Report generation deferred because unfinished STT exists. roomId={}", roomId);
            return;
        }

        List<TranscriptSegment> transcriptSegments = audioQueryPort.loadTranscriptSegmentsByRoomId(roomId).stream()
                .filter(segment -> segment.text() != null && !segment.text().trim().isEmpty())
                .toList();

        String content = transcriptSegments.isEmpty()
                ? EMPTY_TRANSCRIPT_CONTENT
                : meetingSummaryPortOut.summarizeMeeting(room.getTitle(), transcriptSegments);

        Report report = Report.builder()
                .ownerId(room.getHostId())
                .roomId(roomId)
                .content(content)
                .createdTime(LocalDateTime.now())
                .title(room.getTitle())
                .status(true)
                .build();



        try {
            reportCommandPortOut.save(report);
            log.info("Report generated. roomId={}, contentLength={}", roomId, content.length());
        } catch (DataIntegrityViolationException e) {
            log.info("Report creation skipped because another transaction already created it. roomId={}", roomId);
        }
    }
}
