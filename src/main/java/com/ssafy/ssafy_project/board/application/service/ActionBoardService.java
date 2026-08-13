package com.ssafy.ssafy_project.board.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.board.adapter.out.persistence.ActionItemJpaEntity;
import com.ssafy.ssafy_project.board.adapter.out.persistence.ActionItemJpaRepository;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiAssistClient;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 액션아이템 칸반 보드.
 * 회의 종료 후 첫 조회 시 트랜스크립트에서 LLM으로 액션아이템을 추출해 저장하고,
 * 이후에는 상태(할 일/진행 중/완료)를 옮기며 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionBoardService {

    private static final int MAX_TRANSCRIPT_CHARS = 12_000;

    private final ActionItemJpaRepository actionItemRepository;
    private final AudioTextJpaRepository audioTextJpaRepository;
    private final LoadRoomPortOut loadRoomPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final OpenAiAssistClient assistClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<ActionItemJpaEntity> getBoard(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        Room room = loadRoomPortOut.loadById(roomId);
        if (!actionItemRepository.existsByRoomId(roomId) && room.getStatus() == RoomStatus.ENDED) {
            generate(room);
        }
        return actionItemRepository.findAllByRoomIdOrderByIdAsc(roomId);
    }

    @Transactional
    public ActionItemJpaEntity updateStatus(Long itemId, Long userId, String status) {
        ActionItemJpaEntity item = actionItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));

        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(item.getRoomId(), userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        ActionItemJpaEntity.Status newStatus;
        try {
            newStatus = ActionItemJpaEntity.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        item.changeStatus(newStatus);
        return item;
    }

    private void generate(Room room) {
        StringBuilder transcript = new StringBuilder();
        audioTextJpaRepository.findTranscriptSegmentsByRoomIdOrderByAudioTime(room.getId()).forEach(segment -> {
            if (segment.getText() != null && !segment.getText().isBlank()) {
                transcript.append(segment.getSpeakerName()).append(": ")
                        .append(segment.getText().trim()).append('\n');
            }
        });
        if (transcript.isEmpty()) {
            return;
        }
        String transcriptText = transcript.length() > MAX_TRANSCRIPT_CHARS
                ? transcript.substring(transcript.length() - MAX_TRANSCRIPT_CHARS)
                : transcript.toString();

        try {
            String json = assistClient.extractActionItemsJson(room.getTitle(), transcriptText);
            JsonNode itemsNode = objectMapper.readTree(json).path("actionItems");

            List<ActionItemJpaEntity> items = new ArrayList<>();
            for (JsonNode node : itemsNode) {
                String task = node.path("task").asString("").trim();
                if (task.isBlank()) {
                    continue;
                }
                items.add(new ActionItemJpaEntity(
                        room.getId(),
                        node.path("assignee").asString("미정"),
                        task,
                        node.path("due").asString("미정")
                ));
            }
            actionItemRepository.saveAll(items);
            log.info("Action items generated. roomId={}, count={}", room.getId(), items.size());
        } catch (Exception e) {
            log.error("Action item generation failed. roomId={}", room.getId(), e);
        }
    }
}
