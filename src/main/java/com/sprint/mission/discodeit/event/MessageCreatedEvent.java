package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// 채널에 새로운 메시지가 생성될 시 알림을 요청하는 이벤트 클래스
@Getter
public class MessageCreatedEvent {

    // 생성된 메시지 ID
    private final UUID messageId;

    // 메시지가 생성된 채널의 ID
    private final UUID channelId;

    private final Message message;

    // 메시지가 생성된 채널의 Type
    private final ChannelType channelType;

    // 이벤트 생성 시간
    private final Instant occurredAt;

    // 이벤트 생성자
    public MessageCreatedEvent(
            UUID messageId,
            UUID channelId,
            ChannelType channelType,
            Message message
    ) {
        if (messageId == null) {
            throw new InvalidInputException("messageId", null);
        }
        if (channelId == null) {
            throw new InvalidInputException("channelId", null);
        }
        if (channelType == null) {
            throw new InvalidInputException("channelType", null);
        }

        this.messageId = messageId;
        this.channelId = channelId;
        this.channelType = channelType;
        this.message = message;

        this.occurredAt = Instant.now();
    }
}
