package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginRequest {
    private String username;
    private String password;
}
