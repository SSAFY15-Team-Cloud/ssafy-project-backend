package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaRepository;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.audio.application.port.out.PublishTranscriptPortOut;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;

/**
 * STT가 끝난 세그먼트를 회의 참가자들에게 실시간 자막으로 브로드캐스트한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptBroadcastService {

    private final AudioJpaRepository audioJpaRepository;
    private final AudioTextJpaRepository audioTextJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PublishTranscriptPortOut publishTranscriptPortOut;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSttCompleted(AudioSttCompletedEvent event) {
        try {
            AudioJpaEntity audio = audioJpaRepository.findById(event.audioId()).orElse(null);
            if (audio == null) {
                return;
            }

            AudioTextJpaEntity audioText = audioTextJpaRepository
                    .findFirstByAudioIdOrderByCreatedTimeDesc(event.audioId())
                    .orElse(null);
            if (audioText == null || audioText.getText() == null || audioText.getText().isBlank()) {
                return;
            }

            String speakerName = userJpaRepository.findById(audio.getSpeakerId())
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getName())
                    .orElse("speaker-" + audio.getSpeakerId());

            publishTranscriptPortOut.publish(new PublishTranscriptPortOut.TranscriptBroadcast(
                    audio.getRoomId(),
                    audio.getId(),
                    audio.getSpeakerId(),
                    speakerName,
                    audioText.getText(),
                    audio.getStartTime(),
                    audio.getEndTime()
            ));
        } catch (Exception e) {
            // 자막 브로드캐스트 실패가 STT 파이프라인을 되돌리면 안 된다
            log.error("Failed to broadcast transcript. audioId={}", event.audioId(), e);
        }
    }
}
