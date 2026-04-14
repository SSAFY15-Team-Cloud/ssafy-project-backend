package com.ssafy.ssafy_project.audio.adapter.in.web;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.CreateAudioRequest;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataCommand;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.ProcessRoomAudiosPortIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audios")
@RequiredArgsConstructor
public class AudioController {

    private final CreateAudioMetadataPortIn createAudioMetadataPortIn;
    private final ProcessRoomAudiosPortIn processRoomAudiosPortIn;

    @PostMapping("/{roomId}/audios")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createAudio(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateAudioRequest request
    ) {
        CreateAudioMetadataCommand command = new CreateAudioMetadataCommand(
                roomId,
                request.getSpeakerId(),
                request.getPath(),
                request.getMimeType(),
                request.getDuration(),
                request.getFileSize(),
                request.getStartTime(),
                request.getEndTime()
        );

        createAudioMetadataPortIn.create(command);
    }

    @PostMapping("/{roomId}/audios/stt")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processRoomAudios(@PathVariable Long roomId) {
        processRoomAudiosPortIn.processRoomAudios(roomId);
    }
}
