package com.sprint.mission.discodeit.dto.userstatus.response;

import java.time.Instant;
import java.util.UUID;

public record UserStatusResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        UUID userId,
        Instant lastActiveAt,
        boolean online
) {
}
