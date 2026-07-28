package com.example.v_o_server.domain.feed.repository;

import com.example.v_o_server.domain.answer.entity.VideoReaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface FeedReactionQueryRepository extends Repository<VideoReaction, Long> {

    @Query("""
            select reaction.video.id as videoId, count(reaction.id) as totalCount
            from VideoReaction reaction
            where reaction.video.id in :videoIds
            group by reaction.video.id
            """)
    List<FeedCountProjection> countByVideoIds(@Param("videoIds") Collection<Long> videoIds);

    @Query("""
            select reaction.video.id
            from VideoReaction reaction
            where reaction.user.id = :userId
              and reaction.video.id in :videoIds
            """)
    List<Long> findReactedVideoIds(
            @Param("userId") Long userId,
            @Param("videoIds") Collection<Long> videoIds
    );
}
