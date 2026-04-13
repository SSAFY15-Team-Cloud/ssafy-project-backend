package com.ssafy.ssafy_project.audio.application.port.in;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.AudioMetadataResponse;
import com.ssafy.ssafy_project.audio.adapter.in.web.dto.CreateAudioRequest;

public interface CreateAudioMetadataPortIn {
    void create(Long roomId, CreateAudioRequest request);
}