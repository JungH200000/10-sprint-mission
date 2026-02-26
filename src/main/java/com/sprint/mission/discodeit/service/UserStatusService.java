package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.userstatus.response.UserStatusDto;

import java.util.List;
import java.util.UUID;

public interface UserStatusService {
    // creat
    UserStatusDto createUserStatus(UserStatusCreateRequest request);

    // read
    UserStatusDto findUserStatusById(UUID userStatusId);
    UserStatusDto findUserStatusByUserId(UUID userId);

    // all read
    List<UserStatusDto> findAllUserStatus();

    // update
    UserStatusDto updateUserStatus(UUID userStatusId, UserStatusUpdateRequest request);
    UserStatusDto updateUserStatusByUserId(UUID userId, UserStatusUpdateRequest request);

    // delete
    void deleteUserStatus(UUID userStatusId);

}
