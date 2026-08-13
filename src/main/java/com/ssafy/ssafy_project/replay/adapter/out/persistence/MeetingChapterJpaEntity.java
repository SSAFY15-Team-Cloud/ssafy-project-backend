package com.ssafy.ssafy_project.replay.adapter.out.persistence;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 회의별 AI 챕터 캐시 (JSON 원문 저장) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting_chapters", uniqueConstraints = @UniqueConstraint(name = "meeting_chapters_room_id_uk", columnNames = "room_id"))
public class MeetingChapterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "chapters_json", nullable = false, columnDefinition = "TEXT")
    private String chaptersJson;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public MeetingChapterJpaEntity(Long roomId, String chaptersJson) {
        this.roomId = roomId;
        this.chaptersJson = chaptersJson;
    }
}
