package com.sprint.mission.discodeit.dto.channel.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicChannelCreateRequest(
//        @NotNull(message = "ID가 null입니다.")
//        UUID ownerId,

        @NotBlank(message = "name이 입력되지 않았습니다.")
        @Size(min = 1, max = 20)
        String name,

        @NotBlank(message = "description이 입력되지 않았습니다.")
        @Size(max = 100)
        String description
) {
}
