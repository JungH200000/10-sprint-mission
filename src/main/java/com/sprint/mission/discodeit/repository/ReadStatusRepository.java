package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, UUID> {
    Optional<ReadStatus> findByUserIdAndChannelId(UUID userId, UUID channelId);

    List<ReadStatus> findAllByUser_Id(UUID userId);
    List<ReadStatus> findAllByChannel_Id(UUID channelId);

    void deleteByChannel_Id(UUID channelId);

    boolean existsReadStatusByUser_IdAndChannel_Id(UUID userId, UUID channelId);
}
