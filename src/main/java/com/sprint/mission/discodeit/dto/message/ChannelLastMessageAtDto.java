package com.sprint.mission.discodeit.dto.message;

import java.time.Instant;
import java.util.UUID;

public record ChannelLastMessageAtDto(
        UUID id, // channelId
        Instant lastMessageAt
) {
}
