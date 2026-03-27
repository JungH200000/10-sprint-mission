package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class InvalidPasswordException extends UserException {

    public InvalidPasswordException(String password) {
        super(ErrorCode.INVALID_PASSWORD, "password", password);
    }
}
