package com.ssafy.ssafy_project.audio.adapter.in.web.dto;

public record AudioSttRequestMessage(
        Long audioId,
        Long roomId
) {
}