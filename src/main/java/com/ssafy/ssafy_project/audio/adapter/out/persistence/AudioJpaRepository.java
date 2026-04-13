package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioJpaRepository extends JpaRepository<AudioJpaEntity, Long> {
}
