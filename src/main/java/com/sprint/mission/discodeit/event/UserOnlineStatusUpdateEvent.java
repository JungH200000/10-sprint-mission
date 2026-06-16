package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.dto.channel.ChannelDto;
import com.sprint.mission.discodeit.event.enums.ChangeType;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// 사용자 온라인 상태 변경 시 알림 발생을 요청하는 이벤트 클래스
@Getter
public class UserOnlineStatusUpdateEvent {

    private final ChangeType changeType;
    private final UUID userId;

    // 이벤트 생성 시간
    private final Instant occurredAt;

    // 이벤트 생성자
    public UserOnlineStatusUpdateEvent(ChangeType changeType, UUID userId) {
        this.changeType = changeType;
        this.userId = userId;

        this.occurredAt = Instant.now();
    }
}
