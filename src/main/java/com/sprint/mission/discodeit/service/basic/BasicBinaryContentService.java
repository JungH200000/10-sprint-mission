package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.binarycontent.response.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasicBinaryContentService implements BinaryContentService {
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public BinaryContentDto createBinaryContent(BinaryContentCreateRequest binaryContentCreateRequest) {
        BinaryContent binaryContent = new BinaryContent(
                binaryContentCreateRequest.fileName(),
                binaryContentCreateRequest.contentType(),
                binaryContentCreateRequest.bytes(),
                (long) binaryContentCreateRequest.bytes().length
        );
        binaryContentRepository.save(binaryContent);
        return createBinaryContentDto(binaryContent);
    }

    @Override
    public BinaryContentDto findBinaryContentById(UUID binaryContentId) {
        BinaryContent binaryContent = validateAndGetBinaryContentByBinaryContentId(binaryContentId);
        return createBinaryContentDto(binaryContent);
    }

    @Override
    public List<BinaryContentDto> findAllBinaryContentByIdIn(List<UUID> binaryContentIds) {
        if (binaryContentIds == null || binaryContentIds.isEmpty()) return List.of();

        List<BinaryContentDto> foundBinaryContentList = new ArrayList<>();
        for (UUID binaryContentId : binaryContentIds) {
            BinaryContent binaryContent = validateAndGetBinaryContentByBinaryContentId(binaryContentId);
            foundBinaryContentList.add(createBinaryContentDto(binaryContent));
        }
        return foundBinaryContentList;
    }

    @Override
    public void deleteBinaryContent(UUID binaryContentId) {
        validateBinaryContentByBinaryContentId(binaryContentId);
        binaryContentRepository.delete(binaryContentId);
    }

    private BinaryContentDto createBinaryContentDto(BinaryContent binaryContent) {
        return new BinaryContentDto(
                binaryContent.getId(),
                binaryContent.getCreatedAt(),
                binaryContent.getFileName(),
                binaryContent.getSize(),
                binaryContent.getContentType(),
                binaryContent.getBytes());
    }

    public void validateBinaryContentByBinaryContentId(UUID binaryContentId) {
        ValidationMethods.validateId(binaryContentId);
        binaryContentRepository.findById(binaryContentId)
                .orElseThrow(() -> new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found"));
    }
    public BinaryContent validateAndGetBinaryContentByBinaryContentId(UUID binaryContentId) {
        ValidationMethods.validateId(binaryContentId);
        return binaryContentRepository.findById(binaryContentId)
                .orElseThrow(() -> new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found"));
    }
}
