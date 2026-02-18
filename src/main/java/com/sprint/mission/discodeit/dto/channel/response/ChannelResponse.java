package com.sprint.mission.discodeit.dto.channel.response;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.UUID;

public record ChannelResponse(
        UUID id,
//        UUID ownerId,
        ChannelType type,
        String name,
        String description
//        List<UUID> channelMembersIds
) {
}
