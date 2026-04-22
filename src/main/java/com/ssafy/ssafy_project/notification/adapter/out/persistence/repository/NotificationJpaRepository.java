package com.ssafy.ssafy_project.notification.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {
    List<NotificationJpaEntity> findAllByToIdOrderByCreatedTimeDesc(Long toId);

    List<NotificationJpaEntity> findAllByToIdAndReadAtIsNullOrderByCreatedTimeDesc(Long toId);

    long countByToIdAndReadAtIsNull(Long toId);

    Optional<NotificationJpaEntity> findByIdAndToId(Long id, Long toId);
}
