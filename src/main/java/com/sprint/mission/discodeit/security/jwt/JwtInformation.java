package com.sprint.mission.discodeit.security.jwt;

import com.sprint.mission.discodeit.dto.user.UserDto;
import lombok.Getter;

@Getter
public class JwtInformation {

    UserDto userDto;
    String accessToken;
    String refreshToken;

    public JwtInformation(
            UserDto userDto,
            String accessToken,
            String refreshToken
    ) {
        this.userDto = userDto;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // 토큰 재발급 시 새 토큰으로 교체
    public JwtInformation rotate(
            String accessToken,
            String refreshToken
    ) {
        if (accessToken != null) this.accessToken = accessToken;
        if (refreshToken != null) this.refreshToken = refreshToken;

        return this;
    }
}
