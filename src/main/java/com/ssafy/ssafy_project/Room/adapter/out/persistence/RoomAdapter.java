package com.ssafy.ssafy_project.Room.adapter.out.persistence;

import com.ssafy.ssafy_project.Room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.Room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.Room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.Room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.Room.domain.Room;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RoomAdapter implements SaveRoomPortOut {

    private final RoomJpaRepository roomJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public CreateRoomResponse saveRoom(Room room) {

        UserJpaEntity userJpaEntity = userJpaRepository.getReferenceById(room.getUserId());
        RoomJpaEntity roomJpaEntity = roomJpaRepository.save(new RoomJpaEntity(room.getTitle(), userJpaEntity));
        return new CreateRoomResponse(
                roomJpaEntity.getRoomId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getUserId(),
                roomJpaEntity.getCreatedAt()
                );
    }
}
