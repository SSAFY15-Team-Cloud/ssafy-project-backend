package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {

    private String email;
    private String password;
}
