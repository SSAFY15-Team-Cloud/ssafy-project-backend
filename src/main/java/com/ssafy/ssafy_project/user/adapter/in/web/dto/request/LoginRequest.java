package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;


public record LoginRequest(
        String email,
        String password
) {}
