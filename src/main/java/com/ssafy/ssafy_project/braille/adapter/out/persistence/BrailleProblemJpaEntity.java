package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 점자 문제. room_id가 null이면 개인 AI 복습 문제(출제자 본인만 풀이 가능).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "braille_problems")
public class BrailleProblemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** 출제 원문 (한글/숫자/일부 문장부호) */
    @Column(nullable = false, length = 100)
    private String text;

    /** 정답 셀 JSON 배열: [{"v":점형값,"j":"출처자모"}, ...] */
    @Column(name = "expected_cells_json", nullable = false, columnDefinition = "TEXT")
    private String expectedCellsJson;

    /** AI 복습 문제가 겨냥한 취약 자모 (쉼표 구분, 수업 문제는 null) */
    @Column(name = "review_jamos", length = 100)
    private String reviewJamos;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public BrailleProblemJpaEntity(Long roomId, Long creatorId, String text,
                                   String expectedCellsJson, String reviewJamos) {
        this.roomId = roomId;
        this.creatorId = creatorId;
        this.text = text;
        this.expectedCellsJson = expectedCellsJson;
        this.reviewJamos = reviewJamos;
    }
}
