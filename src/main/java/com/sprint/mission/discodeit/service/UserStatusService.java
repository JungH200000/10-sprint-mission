package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusDto;

import java.util.List;
import java.util.UUID;

public interface UserStatusService {
    // creat
    UserStatusDto create(UserStatusCreateRequest request);

    // read
    UserStatusDto find(UUID userStatusId);
    UserStatusDto findByUserId(UUID userId);

    // all read
    List<UserStatusDto> findAll();

    // update
    UserStatusDto update(UUID userStatusId, UserStatusUpdateRequest request);
    UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request);

    // delete
    void delete(UUID userStatusId);

}
