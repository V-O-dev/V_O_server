package com.example.v_o_server.domain.feed.repository;

import com.example.v_o_server.domain.answer.entity.VideoComment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface FeedCommentQueryRepository extends Repository<VideoComment, Long> {

    @Query("""
            select comment.video.id as videoId, count(comment.id) as totalCount
            from VideoComment comment
            where comment.video.id in :videoIds
              and comment.isDeleted = false
            group by comment.video.id
            """)
    List<FeedCountProjection> countActiveByVideoIds(
            @Param("videoIds") Collection<Long> videoIds
    );
}
