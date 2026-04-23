package com.ssafy.ssafy_project.notification.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {
    List<NotificationJpaEntity> findAllByToUser_IdOrderByCreatedTimeDesc(Long toId);

    List<NotificationJpaEntity> findAllByToUser_IdAndReadAtIsNullOrderByCreatedTimeDesc(Long toId);

    long countByToUser_IdAndReadAtIsNull(Long toId);

    Optional<NotificationJpaEntity> findByIdAndToUser_Id(Long id, Long toId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update NotificationJpaEntity n
            set n.readAt = :readAt
            where n.toUser.id = :toId
            and n.readAt is null
            """)
    int markAllAsRead(@Param("toId") Long toId, @Param("readAt")LocalDateTime readAt);
}
