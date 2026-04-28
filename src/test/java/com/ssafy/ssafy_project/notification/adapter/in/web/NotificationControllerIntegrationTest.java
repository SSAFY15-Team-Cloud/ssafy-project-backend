package com.ssafy.ssafy_project.notification.adapter.in.web;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import com.ssafy.ssafy_project.notification.adapter.out.persistence.repository.NotificationJpaRepository;
import com.ssafy.ssafy_project.notification.domain.NotificationType;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaEntity;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaRepository;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private ReportJpaRepository reportJpaRepository;

    @BeforeEach
    void setUp() {
        notificationJpaRepository.deleteAll();
        reportJpaRepository.deleteAll();
        clearPersistence();
    }

    @Test
    void getNotification_returns_all_notifications_for_authenticated_user() throws Exception {
        UserJpaEntity toUser = saveUser("to-user@test.com", "password123!", "to-user", "To User");
        UserJpaEntity fromUser = saveUser("from-user@test.com", "password123!", "from-user", "From User");
        RoomJpaEntity room = saveRoom("B형 스터디 방", fromUser);

        NotificationJpaEntity inviteNotification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "짱승규님이 B형 스터디 방에 초대하셨습니다.",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        ReportJpaEntity report = reportJpaRepository.save(
                ReportJpaEntity.builder()
                        .ownerId(fromUser.getId())
                        .roomId(room.getId())
                        .content("summary")
                        .createdTime(LocalDateTime.now())
                        .title("회의록")
                        .status(true)
                        .build()
        );

        NotificationJpaEntity reportNotification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.REPORT_DONE,
                        "B형 스터디 방 리포트 요약이 완료되었습니다.",
                        toUser,
                        null,
                        null,
                        report,
                        null
                )
        );
        reportNotification.markAsRead(LocalDateTime.now());

        notificationJpaRepository.save(reportNotification);

        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.notifications[0].id").value(reportNotification.getId()))
                .andExpect(jsonPath("$.notifications[0].type").value("REPORT_DONE"))
                .andExpect(jsonPath("$.notifications[0].content").value("B형 스터디 방 리포트 요약이 완료되었습니다."))
                .andExpect(jsonPath("$.notifications[0].isRead").value(true))
                .andExpect(jsonPath("$.notifications[0].payload.reportId").value(report.getId()))
                .andExpect(jsonPath("$.notifications[1].id").value(inviteNotification.getId()))
                .andExpect(jsonPath("$.notifications[1].type").value("MEETING_INVITE"))
                .andExpect(jsonPath("$.notifications[1].content").value("짱승규님이 B형 스터디 방에 초대하셨습니다."))
                .andExpect(jsonPath("$.notifications[1].isRead").value(false))
                .andExpect(jsonPath("$.notifications[1].payload.roomId").value(room.getId()))
                .andExpect(jsonPath("$.notifications[1].payload.roomCode").value(room.getRoomCode()));
    }

    @Test
    void getNotification_with_is_read_false_returns_only_unread_notifications() throws Exception {
        UserJpaEntity toUser = saveUser("unread-to@test.com", "password123!", "unread-to", "Unread To");
        UserJpaEntity fromUser = saveUser("unread-from@test.com", "password123!", "unread-from", "Unread From");
        RoomJpaEntity room = saveRoom("Unread Room", fromUser);

        NotificationJpaEntity unreadNotification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "안 읽은 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        NotificationJpaEntity readNotification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "읽은 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );
        readNotification.markAsRead(LocalDateTime.now());
        notificationJpaRepository.save(readNotification);

        mockMvc.perform(get("/api/notifications")
                        .param("is_read", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(unreadNotification.getId()))
                .andExpect(jsonPath("$.notifications[0].content").value("안 읽은 알림"))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false));
    }

    @Test
    void countUnreadNotification_returns_only_unread_count() throws Exception {
        UserJpaEntity toUser = saveUser("count-to@test.com", "password123!", "count-to", "Count To");
        UserJpaEntity fromUser = saveUser("count-from@test.com", "password123!", "count-from", "Count From");
        RoomJpaEntity room = saveRoom("Count Room", fromUser);

        notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "첫 번째 안 읽은 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "두 번째 안 읽은 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        NotificationJpaEntity readNotification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "읽은 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );
        readNotification.markAsRead(LocalDateTime.now());
        notificationJpaRepository.save(readNotification);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void readNotification_marks_notification_as_read() throws Exception {
        UserJpaEntity toUser = saveUser("read-to@test.com", "password123!", "read-to", "Read To");
        UserJpaEntity fromUser = saveUser("read-from@test.com", "password123!", "read-from", "Read From");
        RoomJpaEntity room = saveRoom("Read Room", fromUser);

        NotificationJpaEntity notification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "읽을 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isNoContent());

        NotificationJpaEntity updatedNotification = notificationJpaRepository.findById(notification.getId()).orElseThrow();
        assertThat(updatedNotification.getReadAt()).isNotNull();
    }

    @Test
    void readAllNotifications_marks_all_unread_notifications_as_read() throws Exception {
        UserJpaEntity toUser = saveUser("readall-to@test.com", "password123!", "readall-to", "ReadAll To");
        UserJpaEntity fromUser = saveUser("readall-from@test.com", "password123!", "readall-from", "ReadAll From");
        RoomJpaEntity room = saveRoom("ReadAll Room", fromUser);

        NotificationJpaEntity first = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "첫 번째 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        NotificationJpaEntity second = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "두 번째 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isNoContent());

        NotificationJpaEntity updatedFirst = notificationJpaRepository.findById(first.getId()).orElseThrow();
        NotificationJpaEntity updatedSecond = notificationJpaRepository.findById(second.getId()).orElseThrow();

        assertThat(updatedFirst.getReadAt()).isNotNull();
        assertThat(updatedSecond.getReadAt()).isNotNull();
    }

    @Test
    void deleteNotification_removes_notification() throws Exception {
        UserJpaEntity toUser = saveUser("delete-to@test.com", "password123!", "delete-to", "Delete To");
        UserJpaEntity fromUser = saveUser("delete-from@test.com", "password123!", "delete-from", "Delete From");
        RoomJpaEntity room = saveRoom("Delete Room", fromUser);

        NotificationJpaEntity notification = notificationJpaRepository.save(
                new NotificationJpaEntity(
                        NotificationType.MEETING_INVITE,
                        "삭제할 알림",
                        toUser,
                        fromUser,
                        room,
                        null,
                        room.getRoomCode()
                )
        );

        mockMvc.perform(delete("/api/notifications/{notificationId}", notification.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(toUser.getId())))
                .andExpect(status().isNoContent());

        assertThat(notificationJpaRepository.findById(notification.getId())).isEmpty();
    }

    @Test
    void getNotification_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}