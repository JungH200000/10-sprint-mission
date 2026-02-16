package com.sprint.mission.discodeit.dto.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "로그인 정보")
public record LoginRequest(
        @NotBlank(message = "newUsername 입력되지 않았습니다.")
        @Pattern(regexp = "^\\S+$", message = "newUsername 공백이 허용되지 않습니다.")
        String username,

        @NotBlank(message = "password가 입력되지 않았습니다.")
        @Pattern(regexp = "^\\S+$", message = "password는 공백이 허용되지 않습니다.")
        String password
) {
}
