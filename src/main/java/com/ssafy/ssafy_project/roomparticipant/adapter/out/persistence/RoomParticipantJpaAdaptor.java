package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.*;
        import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoomParticipantJpaAdaptor implements SaveRoomParticipantPortOut, FindRoomParticipantPortOut,
        LoadActiveRoomParticipantPortOut, SaveRoomParticipantsPortOut, LoadParticipantsPortOut {
    private final RoomParticipantJpaRepository roomParticipantJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final RoomJpaRepository roomJpaRepository;

    @Override
    public void saveRoomParticipant(RoomParticipant roomParticipant) {
        RoomParticipantJpaEntity roomParticipantJpaEntity = null;
        if (roomParticipant.getId() == null) {
            RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomParticipant.getRoom().getId())
                    .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));

            UserJpaEntity userJpaEntity = userJpaRepository.findById(roomParticipant.getUser().getId())
                    .orElseThrow(() -> new CustomException(CommonErrorCode.USER_NOT_FOUND));
            roomParticipantJpaEntity = new RoomParticipantJpaEntity(roomJpaEntity, userJpaEntity);
            roomParticipantJpaEntity.updateFrom(roomParticipant);
        }
        if (roomParticipant.getId() != null) {
            roomParticipantJpaEntity = roomParticipantJpaRepository.findById(roomParticipant.getId())
                    .orElseThrow(() -> new CustomException(CommonErrorCode.PARTICIPANT_NOT_FOUND));
            roomParticipantJpaEntity.updateFrom(roomParticipant);
        }
        roomParticipantJpaRepository.save(roomParticipantJpaEntity);
    }

    @Override
    public Optional<RoomParticipant> findByRoomAndUser(Room room, User user) {
        return roomParticipantJpaRepository.findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), user.getId())
                .map(r -> toDomain(r,user,room));
    }
    @Override
    public void deactivateActiveParticipantsByRoomId(Long roomId, LocalDateTime now) {
        roomParticipantJpaRepository.closeActiveParticipantsByRoomId(roomId, now);
    }

    @Override
    public void deactivateActiveParticipantsByUserId(Long userId, LocalDateTime now) {
        roomParticipantJpaRepository.closeActiveParticipantsByUserId(userId, now);
    }

    @Override
    public List<RoomParticipant> loadActiveRoomParticipantsByRoomId(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));

        return roomParticipantJpaRepository.findAllByRoomJpaEntity_IdAndIsActiveTrue(roomId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RoomParticipant> loadActiveRoomParticipantsByUserId(Long userId) {
        List<RoomParticipantJpaEntity> roomParticipantJpaEntities = roomParticipantJpaRepository.findAllByUserJpaEntity_IdAndIsActiveTrue(userId);

        return roomParticipantJpaEntities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RoomParticipant> loadAllByRoomIdAndIsActiveTrue(Long roomId) {
        return roomParticipantJpaRepository.findAllByRoomJpaEntity_IdAndIsActiveTrue(roomId)
                .stream()
                .map(this::toDomain)
                .toList();
    }
//
//    @Override
//    public void saveRoomParticipants(List<RoomParticipant> roomParticipants) {
//        for (RoomParticipant rp : roomParticipants) {
//            RoomParticipantJpaEntity roomParticipantJpaEntity = roomParticipantJpaRepository.findById(rp.getId())
//                    .orElseThrow(() -> new RuntimeException("방 참가자를 찾을 수 없습니다."));
//            roomParticipantJpaEntity.updateFrom(rp);
//            roomParticipantJpaRepository.save(roomParticipantJpaEntity);
//        }
//    }

    public boolean existsByRoom_IdAndUser_IdAndIsActiveTrue(Long roomId, Long userId) {
        return roomParticipantJpaRepository.existsByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(roomId, userId);
    }

    @Override
    public boolean existsByRoomIdAndUserId(Long roomId, Long userId) {
        return roomParticipantJpaRepository.existsByRoomJpaEntity_IdAndUserJpaEntity_Id(roomId, userId);
    }

    @Override
    public Optional<RoomParticipant> findByRoomAndUserAndIsActiveTrue(Room room, User user) {
        return roomParticipantJpaRepository.findByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(room.getId(), user.getId())
                .map(r -> toDomain(r, user, room));
    }

    private RoomParticipant toDomain(RoomParticipantJpaEntity roomParticipantJpaEntity) {
        return new RoomParticipant(
                roomParticipantJpaEntity.getId(),
                toDomain(roomParticipantJpaEntity.getRoomJpaEntity()),
                toDomain(roomParticipantJpaEntity.getUserJpaEntity()),
                roomParticipantJpaEntity.getRole(),
                roomParticipantJpaEntity.getJoinedTime(),
                roomParticipantJpaEntity.getDurationTime(),
                roomParticipantJpaEntity.getCreatedTime(),
                roomParticipantJpaEntity.isActive()
        );
    }


    private RoomParticipant toDomain(RoomParticipantJpaEntity roomParticipantJpaEntity, User user, Room room) {
        return new RoomParticipant(
                roomParticipantJpaEntity.getId(),
                room,
                user,
                roomParticipantJpaEntity.getRole(),
                roomParticipantJpaEntity.getJoinedTime(),
                roomParticipantJpaEntity.getDurationTime(),
                roomParticipantJpaEntity.getCreatedTime(),
                roomParticipantJpaEntity.isActive()
        );
    }

    private User toDomain(UserJpaEntity userJpaEntity) {
        return new User(
                userJpaEntity.getId(),
                userJpaEntity.getEmail(),
                userJpaEntity.getPassword(),
                userJpaEntity.getRole(),
                userJpaEntity.getNickname(),
                userJpaEntity.getName(),
                userJpaEntity.getProfileImageKey(),
                userJpaEntity.isDeleted()
        );
    }

    private Room toDomain(RoomJpaEntity roomJpaEntity) {
        return new Room(
                roomJpaEntity.getId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
    }
}
