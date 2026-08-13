package com.ssafy.ssafy_project.poll.adapter.out.persistence;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "poll_votes", uniqueConstraints = @UniqueConstraint(name = "poll_votes_poll_user_uk", columnNames = {"poll_id", "user_id"}))
public class PollVoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;

    public PollVoteJpaEntity(Long pollId, Long userId, int optionIndex) {
        this.pollId = pollId;
        this.userId = userId;
        this.optionIndex = optionIndex;
    }

    public void changeOption(int newIndex) {
        this.optionIndex = newIndex;
    }
}
