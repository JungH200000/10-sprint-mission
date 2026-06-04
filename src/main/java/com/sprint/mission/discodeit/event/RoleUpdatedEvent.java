package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import lombok.Getter;

import java.time.Instant;

// 권한 변경시 알림 발생을 요청하는 이벤트 클래스
@Getter
public class RoleUpdatedEvent {

    private final User user;

    // 이벤트 생성 시간
    private final Instant occurredAt;

    // 이벤트 생성자
    public RoleUpdatedEvent(
            User user
    ) {
        if (user == null) {
            throw new InvalidInputException("user", null);
        }

        this.user = user; 
        this.occurredAt = Instant.now();
    }
}
