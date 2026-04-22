package com.ssafy.ssafy_project.audio.adapter.in.web;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.AudioStatusResponse;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.audio.application.port.in.ProcessRoomAudiosPortIn;
import com.ssafy.ssafy_project.audio.domain.Audio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audios")
@RequiredArgsConstructor
public class AudioController {

    private final AudioQueryPort audioQueryPort;
    private final ProcessRoomAudiosPortIn processRoomAudiosPortIn;

    @GetMapping("/{audioId}")
    public ResponseEntity<AudioStatusResponse> getAudioStatus(@PathVariable Long audioId) {
        Audio audio = audioQueryPort.loadById(audioId);
        return ResponseEntity.ok(new AudioStatusResponse(audio.getSttStatus().name()));
    }

    @PostMapping("/{roomId}/audios/stt")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processRoomAudios(@PathVariable Long roomId) {
        processRoomAudiosPortIn.processRoomAudios(roomId);
    }
}
