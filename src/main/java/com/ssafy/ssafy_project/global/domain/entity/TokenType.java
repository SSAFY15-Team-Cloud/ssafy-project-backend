package com.ssafy.ssafy_project.global.domain.entity;

import lombok.Getter;

@Getter
public enum TokenType {

    ACCESS_TOKEN("access"),
    REFRESH_TOKEN("refresh");

    private final String typeName;

    private TokenType(String typeName) {
        this.typeName = typeName;
    }
}
