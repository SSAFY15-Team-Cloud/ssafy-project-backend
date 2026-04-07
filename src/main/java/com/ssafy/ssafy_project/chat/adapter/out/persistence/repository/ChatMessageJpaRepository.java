package com.ssafy.ssafy_project.chat.adapter.out.persistence.repository;

import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    List<ChatMessageJpaEntity> findAllByRoomJpaEntity_IdAndIsDeletedFalseOrderByCreatedTimeAscIdAsc(Long roomId);
}
