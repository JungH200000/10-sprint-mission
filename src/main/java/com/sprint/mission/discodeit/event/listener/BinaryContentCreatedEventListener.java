package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.service.BinaryContentService;
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
    private final BinaryContentService binaryContentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBinaryContentCreatedListener(
            BinaryContentCreatedEvent event
    ) {
        UUID binaryContentId = event.getBinaryContentId();
        byte[] bytes = event.getBytes();

        try {
            // Binary 데이터를 저장소에 저장
            binaryContentStorage.put(binaryContentId, bytes);

        } catch (Exception e) {
            // 저장(put) 실패 시
            binaryContentService.updateStatus(binaryContentId, BinaryContentStatus.FAIL);

            log.error("[BINARY_CONTENT_UPLOAD_FAIL] Binary 파일 저장 실패: binaryContentId={}, status={}, size={}",
                    binaryContentId, BinaryContentStatus.FAIL, bytes.length, e);
        }

        // 저장(put) 성공 시 BinaryContent status를 SUCCESS로 업데이트
        binaryContentService.updateStatus(binaryContentId, BinaryContentStatus.SUCCESS);

        log.debug("[BINARY_CONTENT_UPLOAD_SUCCESS] Binary 파일 저장 완료: binaryContentId={}, status={}, size={}",
                binaryContentId, BinaryContentStatus.SUCCESS, bytes.length);
    }
}
