package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.dto.sse.SseMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// 이벤트 유실 복원을 위해 SSE 메시지를 저장하는 컴포넌트
@Repository
public class SseMessageRepository {

    private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();

    // 최대 저장 이벤트 개수
    private static final int MAX_EVENT_COUNT = 1000;

    public void save(SseMessage sseMessage) {
        eventIdQueue.addLast(sseMessage.id());
        messages.put(sseMessage.id(), sseMessage);

        // 오래된 이벤트(id, message) 삭제
        if (eventIdQueue.size() > MAX_EVENT_COUNT) {
            UUID oldEventId = eventIdQueue.pollFirst();
            if (oldEventId != null) {
                messages.remove(oldEventId);
            }
        }
    }

    // lastEventId 이후의 이벤트 가져오기
    public List<SseMessage> findAllAfter(UUID receiverId, UUID lastEventId) {
        List<SseMessage> result = new ArrayList<>();
        boolean foundLastEventId = false;

        for (UUID eventId : eventIdQueue) {
            // lastEventId 발견 시
            if (eventId.equals(lastEventId)) {
                foundLastEventId = true;
                continue;
            }

            // lastEventId 이전 id일 경우 false라서 continue 동작
            // lastEventId 이후 id일 경우 true라서 다음 로직 실행
            if (!foundLastEventId) {
                continue;
            }

            SseMessage message = messages.get(eventId);

            // message가 null이 아니거나 receiverId가 message에 등록된 수령인일 경우
            if (message != null && message.receiverIds().contains(receiverId)) {
                result.add(message);
            }
        }

        return result;
    }
}
