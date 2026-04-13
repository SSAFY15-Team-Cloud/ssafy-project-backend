package com.ssafy.ssafy_project.audio.adapter.in.web;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.AudioMetadataResponse;
import com.ssafy.ssafy_project.audio.adapter.in.web.dto.CreateAudioRequest;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audios")
@RequiredArgsConstructor
public class AudioController {

    private final CreateAudioMetadataPortIn createAudioMetadataPortIn;

    @PostMapping("/{roomId}/audios")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createAudio(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateAudioRequest request
    ) {
        createAudioMetadataPortIn.create(roomId, request);
    }


}