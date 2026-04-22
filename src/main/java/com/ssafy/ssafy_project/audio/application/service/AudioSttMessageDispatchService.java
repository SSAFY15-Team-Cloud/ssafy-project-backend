package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.application.event.AudioCreatedEvent;
import com.ssafy.ssafy_project.audio.application.port.out.AudioSttMessagePortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioSttMessageDispatchService {

    private final AudioSttMessagePortOut audioSttMessagePortOut;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(AudioCreatedEvent event) {
        audioSttMessagePortOut.send(event.audioId(), event.roomId());
        log.info("Published STT request message. audioId={}, roomId={}", event.audioId(), event.roomId());
    }
}
