package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaEntity;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaRepository;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyRoomHistoryService {

    private final RoomParticipantJpaRepository roomParticipantJpaRepository;
    private final ReportJpaRepository reportJpaRepository;

    public List<MyRoomEntry> getMyRooms(Long userId) {
        List<RoomParticipantJpaEntity> participations =
                roomParticipantJpaRepository.findAllWithRoomByUserId(userId);

        List<Long> roomIds = participations.stream()
                .map(p -> p.getRoomJpaEntity().getId())
                .toList();

        Map<Long, ReportJpaEntity> reports = reportJpaRepository.findAllByRoomIdIn(roomIds).stream()
                .collect(Collectors.toMap(ReportJpaEntity::getRoomId, Function.identity()));

        return participations.stream()
                .map(participation -> {
                    RoomJpaEntity room = participation.getRoomJpaEntity();
                    ReportJpaEntity report = reports.get(room.getId());
                    return new MyRoomEntry(
                            room.getId(),
                            room.getTitle(),
                            room.getRoomCode(),
                            room.getStatus().name(),
                            participation.getRole().name(),
                            participation.getJoinedTime(),
                            room.getEndedTime(),
                            report != null && report.isStatus() ? "DONE"
                                    : report != null ? "PENDING" : "NONE"
                    );
                })
                .toList();
    }

    public record MyRoomEntry(
            Long roomId,
            String title,
            String roomCode,
            String status,
            String myRole,
            LocalDateTime joinedTime,
            LocalDateTime endedTime,
            String reportStatus
    ) {
    }
}
