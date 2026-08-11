package com.ssafy.ssafy_project.room.adapter.in.web;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.CreateAudioRequest;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataCommand;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlCommand;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlResult;
import com.ssafy.ssafy_project.audio.application.service.SpeakingStatsService;
import com.ssafy.ssafy_project.report.application.port.in.GetReportPortIn;
import com.ssafy.ssafy_project.report.application.port.in.GetReportResult;
import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusPortIn;
import com.ssafy.ssafy_project.report.application.port.in.GetReportStatusResult;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.request.CreateRoomRequest;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.request.UpdateRoomRequest;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.GenerateAudioUploadUrlResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.GetReportResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.GetReportStatusResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.GetRoomAudiosResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.GetRoomResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.IssueRtcTokenResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.UpdateRoomResponse;
import com.ssafy.ssafy_project.room.application.port.in.CreateRoomCommand;
import com.ssafy.ssafy_project.room.application.port.in.CreateRoomPortIn;
import com.ssafy.ssafy_project.room.application.port.in.CreateRoomResult;
import com.ssafy.ssafy_project.room.application.port.in.DeleteRoomCommand;
import com.ssafy.ssafy_project.room.application.port.in.DeleteRoomPortIn;
import com.ssafy.ssafy_project.room.application.port.in.GetRoomCommand;
import com.ssafy.ssafy_project.room.application.port.in.GetRoomPortIn;
import com.ssafy.ssafy_project.room.application.port.in.GetRoomResult;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenCommand;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenPortIn;
import com.ssafy.ssafy_project.room.application.port.in.IssueRtcTokenResult;
import com.ssafy.ssafy_project.room.application.port.in.UpdateRoomCommand;
import com.ssafy.ssafy_project.room.application.port.in.UpdateRoomPortIn;
import com.ssafy.ssafy_project.room.application.port.in.UpdateRoomResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomPortIn createRoomPortIn;
    private final CreateAudioMetadataPortIn createAudioMetadataPortIn;
    private final AudioQueryPort audioQueryPort;
    private final UpdateRoomPortIn updateRoomPortIn;
    private final DeleteRoomPortIn deleteRoomPortIn;
    private final GetRoomPortIn getRoomPortIn;
    private final GenerateAudioUploadUrlPortIn generateAudioUploadUrlPortIn;
    private final GetReportPortIn getReportPortIn;
    private final GetReportStatusPortIn getReportStatusPortIn;
    private final IssueRtcTokenPortIn issueRtcTokenPortIn;
    private final SpeakingStatsService speakingStatsService;

    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(
            @RequestBody CreateRoomRequest createRoomRequest,
            @AuthenticationPrincipal Long userId) {
        CreateRoomCommand createRoomCommand = new CreateRoomCommand(createRoomRequest.title(), userId);
        CreateRoomResult createRoomResult = createRoomPortIn.createRoom(createRoomCommand);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateRoomResponse(
                        createRoomResult.roomId(),
                        createRoomResult.title(),
                        createRoomResult.hostId(),
                        createRoomResult.roomCode(),
                        createRoomResult.createdTime()
                ));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<UpdateRoomResponse> updateRoom(
            @RequestBody UpdateRoomRequest updateRoomRequest,
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        UpdateRoomCommand updateRoomCommand = new UpdateRoomCommand(roomId, updateRoomRequest.title(), userId);
        UpdateRoomResult updateRoomResult = updateRoomPortIn.updateRoom(updateRoomCommand);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new UpdateRoomResponse(
                        updateRoomResult.roomId(),
                        updateRoomResult.title()
                ));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> closeRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        DeleteRoomCommand deleteRoomCommand = new DeleteRoomCommand(roomId, userId);
        deleteRoomPortIn.deleteRoom(deleteRoomCommand);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{roomId}/audios")
    public ResponseEntity<Void> createAudio(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateAudioRequest request
    ) {
        CreateAudioMetadataCommand command = new CreateAudioMetadataCommand(
                roomId,
                userId,
                request.getPath(),
                request.getMimeType(),
                request.getDuration(),
                request.getFileSize(),
                request.getStartTime(),
                request.getEndTime()
        );

        createAudioMetadataPortIn.create(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/audios")
    public ResponseEntity<GetRoomAudiosResponse> getRoomAudios(@PathVariable Long roomId) {
        return ResponseEntity.ok(new GetRoomAudiosResponse(
                roomId,
                audioQueryPort.loadAudioIdsByRoomId(roomId)
        ));
    }

    @GetMapping("/{roomId}/audios/upload-url")
    public ResponseEntity<GenerateAudioUploadUrlResponse> generateAudioUploadUrl(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "extension", required = false) String extension
    ) {
        GenerateAudioUploadUrlResult result = generateAudioUploadUrlPortIn.generateUrl(
                new GenerateAudioUploadUrlCommand(roomId, userId, extension)
        );

        return ResponseEntity.ok(new GenerateAudioUploadUrlResponse(result.uploadUrl()));
    }

    @PostMapping("/{roomId}/rtc-token")
    public ResponseEntity<IssueRtcTokenResponse> issueRtcToken(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        IssueRtcTokenResult result = issueRtcTokenPortIn.issueToken(new IssueRtcTokenCommand(roomId, userId));
        return ResponseEntity.ok(new IssueRtcTokenResponse(
                result.serverUrl(),
                result.token(),
                result.roomName(),
                result.identity(),
                result.displayName()
        ));
    }

    @GetMapping("/{roomId}/speaking-stats")
    public ResponseEntity<java.util.List<SpeakingStatsService.SpeakerStat>> getSpeakingStats(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(speakingStatsService.getStats(roomId, userId));
    }

    @GetMapping("/{roomId}/report/status")
    public ResponseEntity<GetReportStatusResponse> getReportStatus(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        GetReportStatusResult result = getReportStatusPortIn.getStatus(roomId, userId);
        return ResponseEntity.ok(new GetReportStatusResponse(result.status()));
    }

    @GetMapping("/{roomId}/report")
    public ResponseEntity<GetReportResponse> getReport(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        GetReportResult result = getReportPortIn.getReport(roomId, userId);
        return ResponseEntity.ok(new GetReportResponse(
                result.reportId(),
                result.ownerId(),
                result.roomId(),
                result.content(),
                result.createdTime(),
                result.title()
        ));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<GetRoomResponse> getRoom(
            @PathVariable String roomCode
    ) {
        GetRoomCommand getRoomCommand = new GetRoomCommand(roomCode);
        GetRoomResult getRoomResult = getRoomPortIn.getRoom(getRoomCommand);

        GetRoomResponse getRoomResponse = new GetRoomResponse(
                getRoomResult.roomId(),
                getRoomResult.title(),
                getRoomResult.status(),
                getRoomResult.hostId(),
                getRoomResult.createdTime()
        );

        return ResponseEntity.ok(getRoomResponse);
    }
}
