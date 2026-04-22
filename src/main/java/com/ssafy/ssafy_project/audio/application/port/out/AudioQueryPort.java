package com.ssafy.ssafy_project.audio.application.port.out;

import com.ssafy.ssafy_project.audio.domain.Audio;

import java.util.List;

public interface AudioQueryPort {

    Audio loadById(Long audioId);

    List<Long> loadAudioIdsByRoomId(Long roomId);

    List<Audio> loadPendingByRoomId(Long roomId);

    boolean existsUnfinishedByRoomId(Long roomId);

    List<TranscriptSegment> loadTranscriptSegmentsByRoomId(Long roomId);
}
