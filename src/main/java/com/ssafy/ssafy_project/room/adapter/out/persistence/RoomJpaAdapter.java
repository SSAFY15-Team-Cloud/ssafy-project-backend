package com.ssafy.ssafy_project.room.adapter.out.persistence;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.UpdateRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomJpaAdapter implements SaveRoomPortOut, UpdateRoomPortOut, LoadRoomPortOut, DeleteRoomPortOut {

    private final RoomJpaRepository roomJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public Room saveRoom(Room room) {
        UserJpaEntity userJpaEntity = userJpaRepository.getReferenceById(room.getHostId());
        RoomJpaEntity roomJpaEntity = roomJpaRepository.save(new RoomJpaEntity(room.getTitle(), userJpaEntity));
        return toDomain(roomJpaEntity);
    }

    @Override
    public Room updateRoom(Room room) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(room.getId())
                .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));

        roomJpaEntity.updateTitle(room.getTitle());
        return toDomain(roomJpaEntity);
    }

    @Override
    public Room loadById(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));
        return toDomain(roomJpaEntity);
    }

    @Override
    public void deleteById(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));
        roomJpaEntity.endRoom();
    }

    @Override
    public Room loadByRoomCode(String roomCode) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));
        return toDomain(roomJpaEntity);
    }

    @Override
    public List<Room> loadRunningRoomsByHostId(Long hostId) {
        return roomJpaRepository.findAllByUserJpaEntity_IdAndStatus(hostId, RoomStatus.RUNNING)
                .stream()
                .map(this::toDomain)
                .toList();
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
