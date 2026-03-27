package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.RoomParticipantCreatePortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;

@Component
@RequiredArgsConstructor
public class RoomParticipantJpaAdaptor implements RoomParticipantCreatePortOut {
    private final RoomParticipantJpaRepository roomParticipantJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    
    
    @Override
    public void createParticipant(RoomParticipant roomParticipant) {
        Long roomId = roomParticipant.getRoomId();
        Long userId = roomParticipant.getUserId();
        
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findByRoomId(roomId)
                .orElseThrow(()-> new RuntimeException("방을 찾을 수 없습니다."));

        UserJpaEntity userJpaEntity = userJpaRepository.findByUserId(userId)
                .orElseThrow(()-> new RuntimeException("유저를 찾을 수 없습니다."));

        RoomParticipantJpaEntity roomParticipantJpaEntity =
                new RoomParticipantJpaEntity(roomJpaEntity, userJpaEntity);

        roomParticipantJpaRepository.save(roomParticipantJpaEntity);
    }
}
