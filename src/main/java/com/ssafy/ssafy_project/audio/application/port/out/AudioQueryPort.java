package com.ssafy.ssafy_project.audio.application.port.out;

import com.ssafy.ssafy_project.audio.domain.Audio;

import java.util.List;

public interface AudioQueryPort {

    Audio loadById(Long audioId);

    List<Audio> loadPendingByRoomId(Long roomId);
}
