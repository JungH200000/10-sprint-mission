package com.sprint.mission.discodeit.dto.binarycontent.response;

import java.time.Instant;
import java.util.UUID;

public record BinaryContentDto(
        UUID id,
        Instant createdAt,
        String fileName,
        Long size,
        String contentType,
        byte[] bytes
) {
}
