package com.ssafy.ssafy_project.audio.application.port.out;

import com.ssafy.ssafy_project.audio.domain.Audio;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;

public interface AudioCommandPort {

    Audio save(Audio audio);

    void updateSttStatus(Long audioId, AudioSttStatus sttStatus);

    void saveTranscription(Long audioId, String text);
}
