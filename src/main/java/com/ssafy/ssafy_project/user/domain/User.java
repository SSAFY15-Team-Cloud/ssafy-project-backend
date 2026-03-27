package com.ssafy.ssafy_project.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String password;
    private UserRole role;
    private String nickname;
    private String name;
    private String profileImageUrl;
    private boolean deleted;

}
