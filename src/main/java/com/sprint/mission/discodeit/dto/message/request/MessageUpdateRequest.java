package com.sprint.mission.discodeit.dto.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "수정할 Message 내용")
public record MessageUpdateRequest(
        @NotBlank(message = "content가 입력되지 않았습니다.")
        String newContent
) {
}
