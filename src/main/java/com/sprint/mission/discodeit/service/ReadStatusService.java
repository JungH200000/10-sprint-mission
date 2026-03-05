package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.readstatus.response.ReadStatusDto;

import java.util.List;
import java.util.UUID;

public interface ReadStatusService {
    // creat
    ReadStatusDto create(ReadStatusCreateRequest readStatusCreateRequest);

    // read
    ReadStatusDto find(UUID readStatusId);

    // all read
    List<ReadStatusDto> findAllByUserId(UUID userId);

    // update
    ReadStatusDto update(UUID readStatusId, ReadStatusUpdateRequest readStatusUpdateRequest);

    // delete
    void delete(UUID readStatusId);
}
