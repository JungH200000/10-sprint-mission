package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.UUID;

public class UserStatusNotFoundException extends UserStatusException {

    public UserStatusNotFoundException(String key, UUID value) {
        super(ErrorCode.USER_STATUS_NOT_FOUND, key, value);
    }
}
