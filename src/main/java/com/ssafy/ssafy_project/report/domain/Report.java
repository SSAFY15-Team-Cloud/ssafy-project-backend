package com.ssafy.ssafy_project.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Report {

    private Long id;
    private Long ownerId;
    private Long roomId;
    private String content;
    private LocalDateTime createdTime;
    private String title;
    private boolean status;
}
