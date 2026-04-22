package com.ssafy.ssafy_project.audio.application.port.out;

public interface AudioSttMessagePortOut {

    void send(Long audioId, Long roomId);
}
