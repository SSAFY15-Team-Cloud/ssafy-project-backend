package com.ssafy.ssafy_project.roomparticipant.application.port.in;

import java.util.List;

public interface GetParticipantsPortIn {
    List<GetParticipantsResult> getParticipants(GetParticipantsCommand getParticipantsCommand);
}
