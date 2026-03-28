package com.ssafy.ssafy_project.user.application.port.in;

import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.SignUpRequest;

public interface SignUpPortIn {
    Long signUp(SignUpCommand signUpCommand);
}
