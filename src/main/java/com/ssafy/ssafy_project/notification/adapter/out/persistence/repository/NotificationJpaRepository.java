package com.ssafy.ssafy_project.notification.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {
    List<NotificationJpaEntity> findAllByToUser_IdOrderByCreatedTimeDesc(Long toId);

    List<NotificationJpaEntity> findAllByToUser_IdAndReadAtIsNullOrderByCreatedTimeDesc(Long toId);

    long countByToUser_IdAndReadAtIsNull(Long toId);

    Optional<NotificationJpaEntity> findByIdAndToUser_Id(Long id, Long toId);
}
