package com.example.v_o_server.domain.answer.entity;

import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "videos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_answer_id", nullable = false)
    private DailyAnswer dailyAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "video_object_key")
    private String videoObjectKey;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "thumbnail_object_key")
    private String thumbnailObjectKey;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "camera_facing")
    private String cameraFacing;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Video(DailyAnswer dailyAnswer, PrivateGroup group, User user, Question question, String videoUrl,
            String videoObjectKey, String thumbnailUrl, String thumbnailObjectKey, String mimeType,
            Long fileSizeBytes, Integer durationMs, Integer width, Integer height, String cameraFacing,
            VideoStatus status, LocalDateTime capturedAt) {
        this.dailyAnswer = dailyAnswer;
        this.group = group;
        this.user = user;
        this.question = question;
        this.videoUrl = videoUrl;
        this.videoObjectKey = videoObjectKey;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailObjectKey = thumbnailObjectKey;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.durationMs = durationMs;
        this.width = width;
        this.height = height;
        this.cameraFacing = cameraFacing;
        this.status = status;
        this.capturedAt = capturedAt;
    }
}
