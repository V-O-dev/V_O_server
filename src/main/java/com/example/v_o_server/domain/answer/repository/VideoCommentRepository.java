package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.VideoComment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    /** 커서(마지막으로 본 commentId) 이후의 삭제되지 않은 댓글을 오래된 순으로 조회한다. */
    List<VideoComment> findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
            Long videoId, Long cursorId, Pageable pageable);
}
