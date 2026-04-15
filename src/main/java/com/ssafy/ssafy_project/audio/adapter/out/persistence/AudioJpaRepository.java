package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AudioJpaRepository extends JpaRepository<AudioJpaEntity, Long> {

    Optional<AudioJpaEntity> findById(Long id);

    List<AudioJpaEntity> findAllByRoomIdAndSttStatus(Long roomId, AudioSttStatus sttStatus);

    boolean existsByRoomIdAndSttStatusIn(Long roomId, Set<AudioSttStatus> sttStatuses);
}
