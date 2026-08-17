package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 자모별 숙련도 (L3 계층). EMA: prof = prof×0.7 + 결과×0.3, 초기값 0.5.
 * 갱신은 동시 제출 레이스를 피하기 위해 BrailleSkillJpaRepository.upsertSkill
 * (원자적 ON CONFLICT upsert)로만 수행한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "braille_skills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_braille_skills_user_jamo", columnNames = {"user_id", "jamo"})
})
public class BrailleSkillJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 자모 또는 학습 단위 기호 (수표, 숫자, 문장부호 포함) */
    @Column(nullable = false, length = 10)
    private String jamo;

    @Column(nullable = false)
    private double proficiency;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "last_wrong_time")
    private LocalDateTime lastWrongTime;

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
}
