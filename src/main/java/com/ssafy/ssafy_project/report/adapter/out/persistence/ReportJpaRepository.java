package com.ssafy.ssafy_project.report.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    boolean existsByRoomId(Long roomId);

    Optional<ReportJpaEntity> findByRoomId(Long roomId);

    List<ReportJpaEntity> findAllByRoomIdIn(Collection<Long> roomIds);

    Optional<ReportJpaEntity> findByShareToken(String shareToken);

    /** 공유 토큰 생성 경합 방지용 잠금 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReportJpaEntity r where r.roomId = :roomId")
    Optional<ReportJpaEntity> findByRoomIdForUpdate(@Param("roomId") Long roomId);
}
