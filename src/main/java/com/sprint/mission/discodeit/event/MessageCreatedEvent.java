package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import lombok.Getter;

import java.time.Instant;

// 채널에 새로운 메시지가 생성될 시 알림 발생을 요청하는 이벤트 클래스
@Getter
public class MessageCreatedEvent {

    private final Message message;

    // 이벤트 생성 시간
    private final Instant occurredAt;

    // 이벤트 생성자
    public MessageCreatedEvent(
            Message message
    ) {
        if (message == null) {
            throw new InvalidInputException("message", null);
        }

        this.message = message;
        this.occurredAt = Instant.now();
    }
}
