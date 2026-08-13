package com.ssafy.ssafy_project.workspace.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의 입장 전 브리핑: 내가 참여했던 가장 최근 종료 회의의 요약과 미완료 액션아이템.
 * LLM 호출 없이 기존 회의록([전체 요약] 섹션)과 액션아이템을 재활용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingBriefingService {

    private final JdbcTemplate jdbcTemplate;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    public Briefing getBriefing(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        // 현재 방을 제외한, 내가 참여했고 리포트가 완성된 가장 최근 회의
        List<LastMeeting> lastMeetings = jdbcTemplate.query("""
                        SELECT r.id, r.title, r.ended_time, rep.content
                        FROM rooms r
                        JOIN report rep ON rep.room_id = r.id AND rep.status = true
                        WHERE r.id <> ?
                          AND r.id IN (SELECT room_id FROM room_participants WHERE user_id = ?)
                        ORDER BY r.ended_time DESC NULLS LAST
                        LIMIT 1
                        """,
                (rs, rowNum) -> new LastMeeting(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getTimestamp("ended_time") != null
                                ? rs.getTimestamp("ended_time").toLocalDateTime() : null,
                        rs.getString("content")
                ),
                roomId, userId);

        if (lastMeetings.isEmpty()) {
            return new Briefing(null, null, null, null, List.of());
        }

        LastMeeting last = lastMeetings.get(0);
        List<OpenItem> openItems = jdbcTemplate.query("""
                        SELECT assignee, task, due, status FROM action_items
                        WHERE room_id = ? AND status <> 'DONE'
                        ORDER BY id ASC
                        LIMIT 6
                        """,
                (rs, rowNum) -> new OpenItem(
                        rs.getString("assignee"),
                        rs.getString("task"),
                        rs.getString("due"),
                        rs.getString("status")
                ),
                last.roomId());

        return new Briefing(
                last.roomId(),
                last.title(),
                last.endedTime(),
                extractSummary(last.content()),
                openItems
        );
    }

    /** 회의록에서 [전체 요약] 섹션만 뽑아낸다 */
    private String extractSummary(String reportContent) {
        if (reportContent == null) {
            return null;
        }
        StringBuilder summary = new StringBuilder();
        boolean inSummary = false;
        for (String line : reportContent.split("\n")) {
            if (line.trim().matches("^\\[.+]$")) {
                if (inSummary) {
                    break;
                }
                inSummary = line.trim().equals("[전체 요약]");
                continue;
            }
            if (inSummary && !line.isBlank()) {
                summary.append(line.trim()).append(' ');
            }
        }
        String result = summary.toString().trim();
        return result.isBlank() ? null : result;
    }

    private record LastMeeting(Long roomId, String title, LocalDateTime endedTime, String content) {
    }

    public record Briefing(
            Long lastRoomId,
            String lastMeetingTitle,
            LocalDateTime lastMeetingEndedTime,
            String lastMeetingSummary,
            List<OpenItem> openActionItems
    ) {
    }

    public record OpenItem(String assignee, String task, String due, String status) {
    }
}
