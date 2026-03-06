package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query(value = "SELECT DISTINCT m FROM Message AS m " +
            "LEFT JOIN FETCH m.channel " +
            "LEFT JOIN FETCH m.author " +
            "LEFT JOIN FETCH m.attachments " +
            "WHERE m.id = :messageId")
    Optional<Message> findByIdWithAuthorAndChannel(@Param("messageId") UUID messageId);

    @Query(value = "SELECT max(m.createdAt) AS lastMessageAt " +
            "FROM Message AS m " +
            "WHERE m.channel.id = :channelId ")
    Optional<Instant> findLastMessageAtByChannelId(@Param("channelId") UUID channelId);

    @Query(value = "SELECT DISTINCT m FROM Message AS m " +
            "LEFT JOIN FETCH m.channel AS c " +
            "LEFT JOIN FETCH m.author AS a " +
            "LEFT JOIN FETCH a.status " +
            "LEFT JOIN FETCH a.profile " +
            "LEFT JOIN FETCH m.attachments " +
            "WHERE c.id = :channelId")
    List<Message> findAllByChannelId(@Param("channelId") UUID channelId);

    @Query(value = "SELECT DISTINCT m FROM Message AS m " +
            "LEFT JOIN FETCH m.channel " +
            "LEFT JOIN FETCH m.author " +
            "LEFT JOIN FETCH m.attachments " +
            "WHERE m.author.id = :authorId")
    List<Message> findAllByAuthorId(@Param("authorId") UUID authorId);

    void deleteAllByChannel_Id(UUID channelId);
}
