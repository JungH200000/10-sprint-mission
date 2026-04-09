package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.UUID;

public class DuplicatedUserStatusException extends UserStatusException {

    public DuplicatedUserStatusException(UUID userId) {
        super(ErrorCode.DUPLICATED_USER_STATUS, "userId", userId);
    }
}
