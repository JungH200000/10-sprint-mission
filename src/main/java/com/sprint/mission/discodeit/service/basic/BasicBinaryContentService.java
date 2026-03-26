package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BasicBinaryContentService implements BinaryContentService {
    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentMapper binaryContentMapper;
    private final BinaryContentStorage binaryContentStorage;

    @Override
    public BinaryContentDto create(BinaryContentCreateRequest binaryContentCreateRequest) {
        log.debug("[BINARY_CONTENT_SAVE] 바이너리 컨텐츠 저장 시작: fileName={}, contentType={}, size={}", binaryContentCreateRequest.fileName(), binaryContentCreateRequest.contentType(), binaryContentCreateRequest.bytes().length);

        byte[] bytes = binaryContentCreateRequest.bytes();
        BinaryContent binaryContent = new BinaryContent(
                binaryContentCreateRequest.fileName(),
                binaryContentCreateRequest.contentType(),
                (long) bytes.length
        );
        binaryContentRepository.save(binaryContent);
        binaryContentStorage.put(binaryContent.getId(), bytes);
        log.info("[BINARY_CONTENT_SAVE] 바이너리 컨텐츠 저장 완료: binaryContentId={}, fileName={}, contentType={}, size={}", binaryContent.getId(), binaryContent.getFileName(), binaryContent.getContentType(), binaryContent.getSize());

        return binaryContentMapper.toDto(binaryContent);
    }

    @Transactional(readOnly = true)
    @Override
    public BinaryContentDto find(UUID binaryContentId) {
        log.debug("[BINARY_CONTENT_FIND] 바이너리 컨텐츠 조회 시작: binaryContentId={}", binaryContentId);

        BinaryContent binaryContent = validateAndGetBinaryContentByBinaryContentId(binaryContentId);
        log.debug("[BINARY_CONTENT_FIND] 바이너리 컨텐츠 조회 완료: binaryContentId={}, fileName={}, contentType={}, size={}", binaryContent.getId(), binaryContent.getFileName(), binaryContent.getContentType(), binaryContent.getSize());

        return binaryContentMapper.toDto(binaryContent);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BinaryContentDto> findAllByIdIn(List<UUID> binaryContentIds) {
        log.debug("[BINARY_CONTENT_LIST_FIND] 바이너리 컨텐츠 목록 조회 시작");

        if (binaryContentIds == null || binaryContentIds.isEmpty()) {
            log.debug("[BINARY_CONTENT_LIST_FIND] 바이너리 컨텐츠 목록 조회 완료: size=0");
            return List.of();
        }

        List<BinaryContentDto> binaryContentDtoList = binaryContentRepository.findAllByIdIn(binaryContentIds).stream()
                .map(binaryContent -> binaryContentMapper.toDto(binaryContent))
                .toList();
        log.debug("[BINARY_CONTENT_LIST_FIND] 바이너리 컨텐츠 목록 조회 완료: size={}", binaryContentDtoList.size());

        return binaryContentDtoList;
    }

    @Override
    public void delete(UUID binaryContentId) {
        log.debug("[BINARY_CONTENT_DELETE] 바이너리 컨텐츠 삭제 시작: binaryContentId={}", binaryContentId);

        validateAndGetBinaryContentByBinaryContentId(binaryContentId);
        binaryContentRepository.deleteById(binaryContentId);
        log.info("[BINARY_CONTENT_DELETE] 바이너리 컨텐츠 삭제 완료: binaryContentId={}", binaryContentId);
    }

    public BinaryContent validateAndGetBinaryContentByBinaryContentId(UUID binaryContentId) {
        ValidationMethods.validateId(binaryContentId);
        return binaryContentRepository.findById(binaryContentId)
                .orElseThrow(() -> new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found"));
    }
}
