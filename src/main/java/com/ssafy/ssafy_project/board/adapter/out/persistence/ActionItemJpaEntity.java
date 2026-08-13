package com.ssafy.ssafy_project.board.adapter.out.persistence;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "action_items")
public class ActionItemJpaEntity {

    public enum Status {TODO, DOING, DONE}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 50)
    private String assignee;

    @Column(nullable = false, length = 500)
    private String task;

    @Column(length = 50)
    private String due;

    @Column(nullable = false, length = 10)
    private String status;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public ActionItemJpaEntity(Long roomId, String assignee, String task, String due) {
        this.roomId = roomId;
        this.assignee = assignee;
        this.task = task;
        this.due = due;
        this.status = Status.TODO.name();
    }

    public void changeStatus(Status newStatus) {
        this.status = newStatus.name();
    }
}
