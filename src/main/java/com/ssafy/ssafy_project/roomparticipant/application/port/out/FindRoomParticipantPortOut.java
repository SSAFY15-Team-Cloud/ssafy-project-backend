package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.user.domain.User;

import java.util.Optional;

public interface FindRoomParticipantPortOut {
    Optional<RoomParticipant> findByRoomAndUser(Room room, User user);

    Optional<RoomParticipant> findByRoomAndUserAndIsActiveTrue(Room room, User user);

    boolean existsByRoom_IdAndUser_IdAndIsActiveTrue(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
