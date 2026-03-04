package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByChannel_Id(UUID channelId);
    List<Message> findAllByAuthor_Id(UUID authorId);

    void deleteAllByChannel_Id(UUID channelId);
}
