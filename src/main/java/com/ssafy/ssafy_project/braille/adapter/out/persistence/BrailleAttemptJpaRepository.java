package com.ssafy.ssafy_project.braille.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BrailleAttemptJpaRepository extends JpaRepository<BrailleAttemptJpaEntity, Long> {

    List<BrailleAttemptJpaEntity> findAllByProblemIdInOrderByIdAsc(Collection<Long> problemIds);

    List<BrailleAttemptJpaEntity> findAllByProblemIdAndUserIdOrderByIdAsc(Long problemId, Long userId);

    /** 사용자가 정답 처리된 문제 id 집합 (목록 조회 N+1 방지용 배치 쿼리) */
    @Query("""
            select distinct a.problemId from BrailleAttemptJpaEntity a
            where a.problemId in :problemIds and a.userId = :userId and a.correct = true
            """)
    Set<Long> findSolvedProblemIds(@Param("problemIds") Collection<Long> problemIds, @Param("userId") Long userId);
}
