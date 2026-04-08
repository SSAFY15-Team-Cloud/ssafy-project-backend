package com.ssafy.ssafy_project.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class User {

    private final Long id;
    private String email;
    private String password;
    private UserRole role;
    private String nickname;
    private String name;
    private String profileImageKey;
    private boolean deleted;

    public User(String email, String password, String nickname, String name, UserRole role, String profileImageKey) {
        this.id = null;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.role = role;
        this.profileImageKey = profileImageKey;
    }

}
