package com.ssafy.ssafy_project.report.application.service;

import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.room.application.event.RoomTerminatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReportGenerationCoordinator {

    private final ReportGenerationService reportGenerationService;

    @Async("reportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomTerminated(RoomTerminatedEvent event) {
        reportGenerationService.generateIfReady(event.roomId());
    }

    @Async("reportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAudioSttCompleted(AudioSttCompletedEvent event) {
        reportGenerationService.generateIfReady(event.roomId());
    }
}
