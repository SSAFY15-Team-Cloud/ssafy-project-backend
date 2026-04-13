package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.audio.domain.AudioUploadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AudioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "speaker_id", nullable = false)
    private Long speakerId;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    private AudioUploadStatus uploadStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "stt_status", nullable = false)
    private AudioSttStatus sttStatus;

}