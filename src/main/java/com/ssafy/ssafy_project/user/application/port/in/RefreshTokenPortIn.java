package com.ssafy.ssafy_project.user.application.port.in;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.RefreshTokenRequest;

public interface RefreshTokenPortIn {
    Tokens reissue(String refreshToken);

}
