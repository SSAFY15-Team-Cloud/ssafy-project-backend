package com.ssafy.ssafy_project.report.adapter.in.web;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaEntity;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 회의록 공개 공유 링크. 토큰을 아는 사람은 로그인 없이 열람할 수 있다.
 */
@RestController
@RequiredArgsConstructor
public class ReportShareController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReportJpaRepository reportRepository;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @PostMapping("/api/rooms/{roomId}/report/share")
    @Transactional
    public ResponseEntity<ShareResponse> enableShare(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
        // 잠금 조회: 동시 활성화 경합에서도 같은 토큰이 반환되게 한다
        ReportJpaEntity report = reportRepository.findByRoomIdForUpdate(roomId)
                .filter(ReportJpaEntity::isStatus)
                .orElseThrow(() -> new CustomException(CommonErrorCode.REPORT_NOT_FOUND));
        if (report.getShareToken() == null) {
            byte[] bytes = new byte[24];
            RANDOM.nextBytes(bytes);
            report.enableShare(HexFormat.of().formatHex(bytes));
        }
        return ResponseEntity.ok(new ShareResponse(report.getShareToken()));
    }

    @DeleteMapping("/api/rooms/{roomId}/report/share")
    @Transactional
    public ResponseEntity<Void> disableShare(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        loadAuthorized(roomId, userId).disableShare();
        return ResponseEntity.noContent().build();
    }

    /** 공개 조회 (인증 불필요 — SecurityConfig 화이트리스트) */
    @GetMapping("/api/shared/reports/{token}")
    @Transactional(readOnly = true)
    public ResponseEntity<SharedReportResponse> getShared(@PathVariable String token) {
        ReportJpaEntity report = reportRepository.findByShareToken(token)
                .filter(ReportJpaEntity::isStatus)
                .orElseThrow(() -> new CustomException(CommonErrorCode.REPORT_NOT_FOUND));

        return ResponseEntity.ok(new SharedReportResponse(
                report.getTitle(),
                report.getContent(),
                report.getCreatedTime()
        ));
    }

    private ReportJpaEntity loadAuthorized(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
        return reportRepository.findByRoomId(roomId)
                .filter(ReportJpaEntity::isStatus)
                .orElseThrow(() -> new CustomException(CommonErrorCode.REPORT_NOT_FOUND));
    }

    public record ShareResponse(String shareToken) {
    }

    public record SharedReportResponse(String title, String content, LocalDateTime createdTime) {
    }
}
