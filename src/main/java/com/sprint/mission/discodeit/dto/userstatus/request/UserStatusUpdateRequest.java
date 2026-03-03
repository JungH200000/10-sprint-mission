package com.sprint.mission.discodeit.dto.userstatus.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UserStatusUpdateRequest(
        @NotNull(message = "lastOnlineTime이 null로 입력되었습니다.")
        Instant newLastActiveAt
) {
}
