package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioTextJpaRepository extends JpaRepository<AudioTextJpaEntity, Long> {
}