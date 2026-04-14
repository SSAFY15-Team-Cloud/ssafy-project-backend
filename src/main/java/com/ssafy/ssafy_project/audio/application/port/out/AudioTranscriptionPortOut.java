package com.ssafy.ssafy_project.audio.application.port.out;

public interface AudioTranscriptionPortOut {

    String transcribe(byte[] audioBytes, String filename);
}
