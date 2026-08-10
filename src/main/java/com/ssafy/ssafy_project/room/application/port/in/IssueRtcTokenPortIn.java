package com.ssafy.ssafy_project.room.application.port.in;

public interface IssueRtcTokenPortIn {

    IssueRtcTokenResult issueToken(IssueRtcTokenCommand command);
}
