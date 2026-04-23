package com.ssafy.ssafy_project.report.application.port.out;

import com.ssafy.ssafy_project.audio.application.port.out.TranscriptSegment;

import java.util.List;

public interface MeetingSummaryPortOut {

    String summarizeMeeting(String meetingTitle, List<TranscriptSegment> transcriptSegments);
}
