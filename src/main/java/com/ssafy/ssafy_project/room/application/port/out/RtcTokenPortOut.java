package com.ssafy.ssafy_project.room.application.port.out;

public interface RtcTokenPortOut {

    String issueToken(String roomName, String identity, String displayName);

    String serverUrl();
}
