package com.sprint.mission.discodeit.dto.binarycontent.response;

import java.time.Instant;
import java.util.UUID;

public record BinaryContentResponse(
        UUID id,
        Instant createAt,
        String fileName,
        Long size,
        String contentType,
        byte[] bytes
) {
}
