package com.ssafy.ssafy_project.report.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "report",
        uniqueConstraints = {
                @UniqueConstraint(name = "report_room_id_uk", columnNames = "room_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "status", nullable = false)
    private boolean status;
}
