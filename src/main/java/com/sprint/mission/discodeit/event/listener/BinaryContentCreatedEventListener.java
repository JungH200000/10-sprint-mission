package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentNotFoundException;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

// BinaryContentCreatedEvent를 받아 Binary 파일 저장을 처리하는 Listener
@Component
@Slf4j
@RequiredArgsConstructor
public class BinaryContentCreatedEventListener {

    // Binary 파일을 저장하는 저장소
    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBinaryContentCreatedListener(
            BinaryContentCreatedEvent event
    ) {
        UUID binaryContentId = event.getBinaryContentId();
        byte[] bytes = event.getBytes();

        // Binary 데이터를 저장소에 저장
        binaryContentStorage.put(binaryContentId, bytes);

        // BinaryContent status를 SUCCESS로 업데이트
        BinaryContent binaryContent = validateAndGetBinaryContentByBinaryContentId(binaryContentId);
        binaryContent.updateBinaryContentStatus(BinaryContentStatus.SUCCESS);

        log.debug("[BINARY_CONTENT_UPLOAD] Binary 파일 저장 완료: binaryContentId={}, status={}, size={}",
                binaryContentId, binaryContent.getStatus().toString(), bytes.length);
    }

    private BinaryContent validateAndGetBinaryContentByBinaryContentId(UUID binaryContentId) {
        if (binaryContentId == null) {
            throw new InvalidInputException("binaryContentId", null);
        }
        return binaryContentRepository.findById(binaryContentId)
                .orElseThrow(() -> new BinaryContentNotFoundException(binaryContentId));
    }
}
