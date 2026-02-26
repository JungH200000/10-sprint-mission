package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.binarycontent.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.binarycontent.response.BinaryContentDto;

import java.util.List;
import java.util.UUID;

public interface BinaryContentService {
    // create
    BinaryContentDto createBinaryContent(BinaryContentCreateRequest binaryContentCreateRequest);

    // find
    BinaryContentDto findBinaryContentById(UUID binaryContentId);

    // all find
    List<BinaryContentDto> findAllBinaryContentByIdIn(List<UUID> binaryContentIds);

    // delete
    void deleteBinaryContent(UUID binaryContentId);
}
