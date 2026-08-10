package com.ssafy.ssafy_project.knowledge.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentJpaRepository extends JpaRepository<KnowledgeDocumentJpaEntity, Long> {

    List<KnowledgeDocumentJpaEntity> findAllByOrderByCreatedTimeDesc();
}
