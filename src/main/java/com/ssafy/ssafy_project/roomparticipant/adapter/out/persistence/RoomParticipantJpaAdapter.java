package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence;

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

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoomParticipantJpaAdapter implements SaveRoomParticipantPortOut, FindRoomParticipantPortOut,
        LoadActiveRoomParticipantPortOut, SaveRoomParticipantsPortOut {
    private final RoomParticipantJpaRepository roomParticipantJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final RoomJpaRepository roomJpaRepository;


    @Override
    public void saveRoomParticipant(RoomParticipant roomParticipant) {
        RoomParticipantJpaEntity roomParticipantJpaEntity= null;
        if(roomParticipant.getId() == null){
            RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomParticipant.getRoom().getId())
                    .orElseThrow(()-> new RuntimeException("방을 찾을 수 없습니다."));

            UserJpaEntity userJpaEntity = userJpaRepository.findById(roomParticipant.getUser().getId())
                    .orElseThrow(()-> new RuntimeException("유저 찾을 수 없습니다."));
            roomParticipantJpaEntity = new RoomParticipantJpaEntity(roomJpaEntity, userJpaEntity);
            roomParticipantJpaEntity.updateFrom(roomParticipant);
        }
        if(roomParticipant.getId() != null){
            roomParticipantJpaEntity = roomParticipantJpaRepository.findById(roomParticipant.getId())
                    .orElseThrow(()-> new RuntimeException("방 참가자를 찾을 수 없습니다."));
            roomParticipantJpaEntity.updateFrom(roomParticipant);
        }
        roomParticipantJpaRepository.save(roomParticipantJpaEntity);
    }

    @Override
    public Optional<RoomParticipant> findByRoomAndUser(Room room, User user) {
        return roomParticipantJpaRepository.findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), user.getId())
                .map(r ->
                    new RoomParticipant(
                        r.getId(),
                        room,
                        user,
                        r.getRole(),
                        r.getJoinedTime(),
                        r.getDurationTime(),
                        r.getCreatedTime(),
                        r.isActive()
                ));
    }

    @Override
    public List<RoomParticipant> loadActiveRoomParticipantsByRoomId(Long roomId) {
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(roomId)
                .orElseThrow(()-> new RuntimeException("방을 찾을 수 없습니다."));
        Room room = new Room(
                roomJpaEntity.getId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
        return roomParticipantJpaRepository.findAllByRoomJpaEntity_IdAndIsActiveTrue(roomId)
                .stream()
                .map(r->{
                    User user = new User(
                            r.getUserJpaEntity().getId(),
                            r.getUserJpaEntity().getEmail(),
                            r.getUserJpaEntity().getPassword(),
                            r.getUserJpaEntity().getRole(),
                            r.getUserJpaEntity().getNickname(),
                            r.getUserJpaEntity().getName(),
                            r.getUserJpaEntity().getProfileImageUrl(),
                            r.getUserJpaEntity().isDeleted()
                    );
                    return new RoomParticipant(
                        r.getId(),
                        room,
                        user,
                        r.getRole(),
                        r.getJoinedTime(),
                        r.getDurationTime(),
                        r.getCreatedTime(),
                        r.isActive()
                );
                })
                .toList();
    }

    @Override
    public void saveRoomParticipants(List<RoomParticipant> roomParticipants) {
        for(RoomParticipant rp : roomParticipants){
            RoomParticipantJpaEntity roomParticipantJpaEntity = roomParticipantJpaRepository.findById(rp.getId())
                    .orElseThrow(()-> new RuntimeException("방 참가자를 찾을 수 없습니다."));
            roomParticipantJpaEntity.updateFrom(rp);
            roomParticipantJpaRepository.save(roomParticipantJpaEntity);
        }
    }

    @Override
    public Optional<RoomParticipant> findByRoomAndUserAndIsActiveTrue(Room room, User user) {
        return roomParticipantJpaRepository.findByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(room.getId(), user.getId())
                .map(r ->
                        new RoomParticipant(
                                r.getId(),
                                room,
                                user,
                                r.getRole(),
                                r.getJoinedTime(),
                                r.getDurationTime(),
                                r.getCreatedTime(),
                                r.isActive()
                        ));
    }
}
