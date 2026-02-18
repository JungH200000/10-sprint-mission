package com.sprint.mission.discodeit.dto.user.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        String email,
        String username,
        String birthday,
        UUID profileId,
        boolean online
) {
}
