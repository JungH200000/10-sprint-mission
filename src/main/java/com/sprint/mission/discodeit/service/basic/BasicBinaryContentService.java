package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.input.BinaryContentCreateRequest;
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
    public BinaryContent createBinaryContent(BinaryContentCreateRequest request) {
        BinaryContent binaryContent = new BinaryContent(
                request.fileName(),
                request.contentType(),
                request.bytes(),
                (long) request.bytes().length
        );
        binaryContentRepository.save(binaryContent);
        return binaryContent;
    }

    @Override
    public BinaryContent findBinaryContentById(UUID binaryContentId) {
        return validateAndGetBinaryContentByBinaryContentId(binaryContentId);
    }

    @Override
    public List<BinaryContent> findAllBinaryContentByIdIn(List<UUID> binaryContentIds) {
        if (binaryContentIds == null || binaryContentIds.isEmpty()) return List.of();

        List<BinaryContent> foundBinaryContentList = new ArrayList<>();
        for (UUID binaryContentId : binaryContentIds) {
            foundBinaryContentList.add(validateAndGetBinaryContentByBinaryContentId(binaryContentId));
        }
        return foundBinaryContentList;
    }

    @Override
    public void deleteBinaryContent(UUID binaryContentId) {
        validateBinaryContentByBinaryContentId(binaryContentId);
        binaryContentRepository.delete(binaryContentId);
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
