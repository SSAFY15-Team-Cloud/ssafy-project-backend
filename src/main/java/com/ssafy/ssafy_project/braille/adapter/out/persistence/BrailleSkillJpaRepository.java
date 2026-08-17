package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrailleSkillJpaRepository extends JpaRepository<BrailleSkillJpaEntity, Long> {

    List<BrailleSkillJpaEntity> findAllByUserId(Long userId);

    /**
     * EMA 갱신을 원자적 upsert 한 번으로 처리한다 (find-or-create 레이스 방지).
     * EMA: prof = prof×0.7 + 결과×0.3, 초기값 0.5 — BrailleSkillJpaEntity 문서와 동일.
     */
    @Modifying
    @Query(value = """
            INSERT INTO braille_skills (user_id, jamo, proficiency, attempt_count, wrong_count, last_wrong_time, updated_time)
            VALUES (:userId, :jamo, 0.5 * 0.7 + (CASE WHEN :correct THEN 1.0 ELSE 0.0 END) * 0.3, 1,
                    CASE WHEN :correct THEN 0 ELSE 1 END,
                    CASE WHEN :correct THEN NULL ELSE now() END, now())
            ON CONFLICT ON CONSTRAINT uk_braille_skills_user_jamo DO UPDATE SET
                proficiency = braille_skills.proficiency * 0.7 + (CASE WHEN :correct THEN 1.0 ELSE 0.0 END) * 0.3,
                attempt_count = braille_skills.attempt_count + 1,
                wrong_count = braille_skills.wrong_count + CASE WHEN :correct THEN 0 ELSE 1 END,
                last_wrong_time = CASE WHEN :correct THEN braille_skills.last_wrong_time ELSE now() END,
                updated_time = now()
            """, nativeQuery = true)
    void upsertSkill(@Param("userId") Long userId, @Param("jamo") String jamo, @Param("correct") boolean correct);
}
