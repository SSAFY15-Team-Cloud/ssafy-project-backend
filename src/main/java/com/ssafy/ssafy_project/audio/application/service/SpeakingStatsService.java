package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaRepository;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeakingStatsService {

    private final AudioJpaRepository audioJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    public List<SpeakerStat> getStats(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        List<AudioJpaRepository.SpeakerDurationProjection> rows =
                audioJpaRepository.sumDurationBySpeaker(roomId);

        Map<Long, UserJpaEntity> users = userJpaRepository
                .findAllById(rows.stream().map(AudioJpaRepository.SpeakerDurationProjection::getSpeakerId).toList())
                .stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, Function.identity()));

        return rows.stream()
                .map(row -> {
                    UserJpaEntity user = users.get(row.getSpeakerId());
                    String name = user != null
                            ? (user.getNickname() != null ? user.getNickname() : user.getName())
                            : "speaker-" + row.getSpeakerId();
                    return new SpeakerStat(row.getSpeakerId(), name,
                            row.getTotalDuration() != null ? row.getTotalDuration() : 0L);
                })
                .toList();
    }

    public record SpeakerStat(Long userId, String name, long totalSeconds) {
    }
}
