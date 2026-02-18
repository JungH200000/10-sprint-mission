package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserStatusService {
    // creat
    UserStatus createUserStatus(UserStatusCreateRequest request);

    // read
    UserStatus findUserStatusById(UUID userStatusId);
    UserStatus findUserStatusByUserId(UUID userId);

    // all read
    List<UserStatus> findAllUserStatus();

    // update
    UserStatus updateUserStatus(UUID userStatusId, UserStatusUpdateRequest request);
    UserStatus updateUserStatusByUserId(UUID userId, Instant lastOnlineTime);

    // delete
    void deleteUserStatus(UUID userStatusId);

}
