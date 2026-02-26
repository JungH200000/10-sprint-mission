package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.readstatus.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.ReadStatus;

import java.util.List;
import java.util.UUID;

public interface ReadStatusService {
    // creat
    ReadStatus createReadStatus(ReadStatusCreateRequest readStatusCreateRequest);

    // read
    ReadStatus findReadStatusById(UUID readStatusId);

    // all read
    List<ReadStatusDto> findAllByUserId(UUID userId);

    // update
    ReadStatus updateReadStatus(UUID readStatusId, ReadStatusUpdateRequest readStatusUpdateRequest);

    // delete
    void deleteReadStatus(UUID readStatusId);
}
