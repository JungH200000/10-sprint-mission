package com.sprint.mission.discodeit.exception.auth;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class LoginFailedException extends AuthException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED, "Login", "Invalid username or password");
    }
}
