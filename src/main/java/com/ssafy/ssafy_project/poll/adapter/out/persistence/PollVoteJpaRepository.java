package com.ssafy.ssafy_project.poll.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollVoteJpaRepository extends JpaRepository<PollVoteJpaEntity, Long> {

    List<PollVoteJpaEntity> findAllByPollId(Long pollId);

    Optional<PollVoteJpaEntity> findByPollIdAndUserId(Long pollId, Long userId);

    /** 동시 첫 투표 경합에서도 안전한 원자적 upsert */
    @Modifying
    @Query(value = """
            INSERT INTO poll_votes (poll_id, user_id, option_index)
            VALUES (:pollId, :userId, :optionIndex)
            ON CONFLICT ON CONSTRAINT poll_votes_poll_user_uk
            DO UPDATE SET option_index = :optionIndex
            """, nativeQuery = true)
    void upsertVote(@Param("pollId") Long pollId, @Param("userId") Long userId, @Param("optionIndex") int optionIndex);
}
