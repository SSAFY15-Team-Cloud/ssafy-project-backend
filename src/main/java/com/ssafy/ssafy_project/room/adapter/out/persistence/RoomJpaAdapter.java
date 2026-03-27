package com.ssafy.ssafy_project.room.adapter.out.persistence;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.UpdateRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RoomJpaAdapter implements SaveRoomPortOut, UpdateRoomPortOut, LoadRoomPortOut, DeleteRoomPortOut {

    private final RoomJpaRepository roomJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public Room saveRoom(Room room) {

        UserJpaEntity userJpaEntity = userJpaRepository.getReferenceById(room.getHostId());
        RoomJpaEntity roomJpaEntity = roomJpaRepository.save(new RoomJpaEntity(room.getTitle(), userJpaEntity));
        return new Room(
                roomJpaEntity.getRoomId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getUserId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
    }

    @Override
    public Room updateRoom(Room room) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findByRoomId(room.getRoomId())
                .orElseThrow(()-> new RuntimeException("해당 방을 찾을 수 없습니다."));


        roomJpaEntity.updateTitle(room.getTitle());

        return new Room(
                roomJpaEntity.getRoomId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getUserId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
    }

    @Override
    public Room loadById(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findByRoomId(roomId)
                .orElseThrow(()-> new RuntimeException("해당 방을 찾을 수 없습니다."));
        return new Room(
                roomJpaEntity.getRoomId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getUserId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
    }

    @Override
    public void deleteRoom(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findByRoomId(roomId)
                .orElseThrow(()-> new RuntimeException("방을 찾을 수 없습니다."));
        roomJpaEntity.endRoom();
    }
}

