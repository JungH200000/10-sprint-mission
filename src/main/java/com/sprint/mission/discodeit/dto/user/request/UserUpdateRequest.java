package com.sprint.mission.discodeit.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
        @Pattern(regexp = "^\\S+$", message = "email은 공백이 허용되지 않습니다.")
        @Email(message = "newEmail 형식에 맞지 않습니다.")
        String newEmail,

        @Pattern(regexp = "^\\S+$", message = "password는 공백이 허용되지 않습니다.")
        String newPassword,

        @Pattern(regexp = "^\\S+$", message = "newUsername 공백이 허용되지 않습니다.")
        String newUsername,

        @Pattern(regexp = "^\\d{8}$", message = "birthday는 YYYYMMDD 형식이어야 합니다.")
        String newBirthday
) {
}
