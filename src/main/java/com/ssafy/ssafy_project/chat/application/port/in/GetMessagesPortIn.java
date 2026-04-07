package com.ssafy.ssafy_project.chat.application.port.in;

public interface GetMessagesPortIn {
    GetMessagesResult getMessages(GetMessagesCommand getMessagesCommand);
}
