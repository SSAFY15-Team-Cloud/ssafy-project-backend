package com.ssafy.ssafy_project.workspace.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 워크스페이스 홈: 내 회의/발언/할일 통계와 주간 활동, 미완료 액션아이템 허브.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceOverviewService {

    private final JdbcTemplate jdbcTemplate;

    public Overview getOverview(Long userId) {
        long totalMeetings = queryLong(
                "SELECT COUNT(*) FROM room_participants WHERE user_id = ?", userId);
        long totalSpeakingSeconds = queryLong(
                "SELECT COALESCE(SUM(duration), 0) FROM audio WHERE speaker_id = ?", userId);

        // 내가 참여한 방들의 액션아이템 현황
        long totalActionItems = queryLong("""
                SELECT COUNT(*) FROM action_items ai
                WHERE ai.room_id IN (SELECT room_id FROM room_participants WHERE user_id = ?)
                """, userId);
        long doneActionItems = queryLong("""
                SELECT COUNT(*) FROM action_items ai
                WHERE ai.room_id IN (SELECT room_id FROM room_participants WHERE user_id = ?)
                  AND ai.status = 'DONE'
                """, userId);

        // 최근 7일 일자별 회의 참여
        Map<LocalDate, Integer> byDay = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT CAST(joined_time AS DATE) AS day, COUNT(*) AS cnt
                        FROM room_participants
                        WHERE user_id = ? AND joined_time >= ?
                        GROUP BY CAST(joined_time AS DATE)
                        """,
                rs -> {
                    byDay.put(rs.getDate("day").toLocalDate(), rs.getInt("cnt"));
                },
                userId, LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay());

        List<DayActivity> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            weekly.add(new DayActivity(day.toString(), byDay.getOrDefault(day, 0)));
        }

        // 미완료 할 일 (방 제목 포함, 최근 회의 우선)
        List<PendingItem> pendingItems = jdbcTemplate.query("""
                        SELECT ai.id, ai.room_id, r.title AS room_title, ai.assignee, ai.task, ai.due, ai.status
                        FROM action_items ai
                        JOIN rooms r ON r.id = ai.room_id
                        WHERE ai.room_id IN (SELECT room_id FROM room_participants WHERE user_id = ?)
                          AND ai.status <> 'DONE'
                        ORDER BY ai.id DESC
                        LIMIT 12
                        """,
                (rs, rowNum) -> new PendingItem(
                        rs.getLong("id"),
                        rs.getLong("room_id"),
                        rs.getString("room_title"),
                        rs.getString("assignee"),
                        rs.getString("task"),
                        rs.getString("due"),
                        rs.getString("status")
                ),
                userId);

        return new Overview(
                totalMeetings,
                totalSpeakingSeconds,
                totalActionItems,
                doneActionItems,
                weekly,
                pendingItems
        );
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value != null ? value : 0L;
    }

    public record Overview(
            long totalMeetings,
            long totalSpeakingSeconds,
            long totalActionItems,
            long doneActionItems,
            List<DayActivity> weeklyActivity,
            List<PendingItem> pendingActionItems
    ) {
    }

    public record DayActivity(String date, int meetings) {
    }

    public record PendingItem(
            Long id,
            Long roomId,
            String roomTitle,
            String assignee,
            String task,
            String due,
            String status
    ) {
    }
}
