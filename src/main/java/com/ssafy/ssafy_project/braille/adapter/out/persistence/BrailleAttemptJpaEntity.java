package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 풀이 시도 (L1 세션 로그 + L2 점 단위 오류 이벤트).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "braille_attempts", indexes = {
        @Index(name = "idx_braille_attempts_problem", columnList = "problem_id"),
        @Index(name = "idx_braille_attempts_user", columnList = "user_id")
})
public class BrailleAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean correct;

    /** 정답 셀 대비 정확 셀 비율 (0.0~1.0) */
    @Column(nullable = false)
    private double accuracy;

    /** 사용자 입력 셀 JSON 배열: [점형값, ...] */
    @Column(name = "cells_json", nullable = false, columnDefinition = "TEXT")
    private String cellsJson;

    /** 점 단위 채점 결과 JSON (BrailleGrader.CellResult 배열) */
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public BrailleAttemptJpaEntity(Long problemId, Long userId, boolean correct, double accuracy,
                                   String cellsJson, String resultJson) {
        this.problemId = problemId;
        this.userId = userId;
        this.correct = correct;
        this.accuracy = accuracy;
        this.cellsJson = cellsJson;
        this.resultJson = resultJson;
    }
}
