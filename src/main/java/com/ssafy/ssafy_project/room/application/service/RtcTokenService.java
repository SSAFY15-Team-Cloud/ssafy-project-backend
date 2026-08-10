package com.ssafy.ssafy_project.room.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenCommand;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenPortIn;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenResult;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.RtcTokenPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RtcTokenService implements IssueRtcTokenPortIn {

    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final RtcTokenPortOut rtcTokenPortOut;

    @Override
    public IssueRtcTokenResult issueToken(IssueRtcTokenCommand command) {
        Room room = loadRoomPortOut.loadById(command.roomId());

        if (!RoomStatus.RUNNING.equals(room.getStatus())) {
            throw new CustomException(CommonErrorCode.ROOM_ALREADY_ENDED);
        }

        boolean isActiveParticipant = findRoomParticipantPortOut
                .existsByRoom_IdAndUser_IdAndIsActiveTrue(room.getId(), command.userId());

        if (!isActiveParticipant) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        User user = loadUserPortOut.loadById(command.userId());
        String identity = String.valueOf(user.getId());
        String displayName = user.getNickname() != null ? user.getNickname() : user.getName();

        String token = rtcTokenPortOut.issueToken(room.getRoomCode(), identity, displayName);

        return new IssueRtcTokenResult(
                rtcTokenPortOut.serverUrl(),
                token,
                room.getRoomCode(),
                identity,
                displayName
        );
    }
}
