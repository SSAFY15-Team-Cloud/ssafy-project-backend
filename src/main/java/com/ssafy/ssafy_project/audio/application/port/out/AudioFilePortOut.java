package com.ssafy.ssafy_project.audio.application.port.out;

public interface AudioFilePortOut {

    byte[] downloadAudio(String fullPath);

    String extractFilename(String fullPath);
}
