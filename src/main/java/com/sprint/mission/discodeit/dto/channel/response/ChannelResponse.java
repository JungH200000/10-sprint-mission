package com.sprint.mission.discodeit.dto.channel.response;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
//        UUID ownerId,
        Instant createdAt,
        Instant updatedAt,
        ChannelType type,
        String name,
        String description
//        List<UUID> channelMembersIds
) {
}
