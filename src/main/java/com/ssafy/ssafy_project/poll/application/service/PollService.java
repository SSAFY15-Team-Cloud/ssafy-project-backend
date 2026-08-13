package com.ssafy.ssafy_project.poll.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.poll.adapter.out.persistence.PollJpaEntity;
import com.ssafy.ssafy_project.poll.adapter.out.persistence.PollJpaRepository;
import com.ssafy.ssafy_project.poll.adapter.out.persistence.PollVoteJpaEntity;
import com.ssafy.ssafy_project.poll.adapter.out.persistence.PollVoteJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;

import java.util.List;

/**
 * 회의 중 실시간 투표. 생성/투표/마감 시 방 전체에 STOMP로 현재 상태를 브로드캐스트한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PollService {

    private final PollJpaRepository pollRepository;
    private final PollVoteJpaRepository voteRepository;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public PollView createPoll(Long roomId, Long userId, String question, List<String> options) {
        requireActiveParticipant(roomId, userId);
        if (question == null || question.isBlank() || options == null
                || options.size() < 2 || options.size() > 6
                || options.stream().anyMatch(option -> option == null || option.isBlank())) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        PollJpaEntity poll = pollRepository.save(new PollJpaEntity(
                roomId, userId, question.trim(), writeJson(options)
        ));

        PollView view = toView(poll, userId);
        broadcast(roomId, view);
        return view;
    }

    public PollView vote(Long pollId, Long userId, int optionIndex) {
        PollJpaEntity poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));
        requireActiveParticipant(poll.getRoomId(), userId);

        List<String> options = readOptions(poll.getOptionsJson());
        if (poll.isClosed() || optionIndex < 0 || optionIndex >= options.size()) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        voteRepository.upsertVote(pollId, userId, optionIndex);

        PollView view = toView(poll, userId);
        broadcast(poll.getRoomId(), view);
        return view;
    }

    public PollView closePoll(Long pollId, Long userId) {
        PollJpaEntity poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));
        if (!poll.getCreatorId().equals(userId)) {
            throw new CustomException(CommonErrorCode.NOT_MESSAGE_AUTHOR);
        }
        poll.close();

        PollView view = toView(poll, userId);
        broadcast(poll.getRoomId(), view);
        return view;
    }

    @Transactional(readOnly = true)
    public List<PollView> getPolls(Long roomId, Long userId) {
        requireActiveParticipant(roomId, userId);
        return pollRepository.findAllByRoomIdOrderByIdDesc(roomId).stream()
                .map(poll -> toView(poll, userId))
                .toList();
    }

    private void requireActiveParticipant(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
    }

    private void broadcast(Long roomId, PollView view) {
        // 브로드캐스트에는 개인 정보(myVote)를 빼고 보낸다
        simpMessagingTemplate.convertAndSend(
                "/sub/rooms/" + roomId + "/polls",
                new PollView(view.pollId(), view.creatorId(), view.question(), view.options(), view.counts(),
                        view.totalVotes(), view.closed(), null)
        );
    }

    private PollView toView(PollJpaEntity poll, Long userId) {
        List<String> options = readOptions(poll.getOptionsJson());
        int[] counts = new int[options.size()];
        Integer myVote = null;
        for (PollVoteJpaEntity vote : voteRepository.findAllByPollId(poll.getId())) {
            if (vote.getOptionIndex() >= 0 && vote.getOptionIndex() < counts.length) {
                counts[vote.getOptionIndex()]++;
            }
            if (vote.getUserId().equals(userId)) {
                myVote = vote.getOptionIndex();
            }
        }
        int total = 0;
        for (int count : counts) {
            total += count;
        }

        List<Integer> countList = new java.util.ArrayList<>();
        for (int count : counts) {
            countList.add(count);
        }

        return new PollView(poll.getId(), poll.getCreatorId(), poll.getQuestion(), options, countList, total,
                poll.isClosed(), myVote);
    }

    private String writeJson(List<String> options) {
        return objectMapper.writeValueAsString(options.stream().map(String::trim).toList());
    }

    private List<String> readOptions(String json) {
        return objectMapper.readValue(json,
                TypeFactory.createDefaultInstance().constructCollectionType(List.class, String.class));
    }

    public record PollView(
            Long pollId,
            Long creatorId,
            String question,
            List<String> options,
            List<Integer> counts,
            int totalVotes,
            boolean closed,
            Integer myVote
    ) {
    }
}
