package com.ssafy.ssafy_project.global.infrastructure.websocket;

import java.security.Principal;

public record StompPrincipal(String userId) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}
