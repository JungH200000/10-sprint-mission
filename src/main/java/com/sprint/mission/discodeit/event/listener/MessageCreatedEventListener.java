package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// MessageCreatedEvent를 받아 메시지 생성 시 알림을 발행하는 Listener
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageCreatedEventListener {

    private final ReadStatusRepository readStatusRepository;

    // 메시지 생성 트랜잭션이 성공적으로 Commit된 뒤 알림을 보내는 Listener
    @TransactionalEventListener
    public void on(MessageCreatedEvent event) {
        Message message = event.getMessage();
        Channel channel = message.getChannel();
        User author = message.getAuthor();

        // 채널 알림 여부를 활성화(true)한 ReadStatus 조회한 후 사용자 ID Set(중복 방지)
        Set<UUID> receiverId = readStatusRepository
                .findAllByChannelIdAndNotificationEnabledIsTrue(channel.getId())
                .stream()
                .map(readStatus -> readStatus.getUser().getId())
                // 메시지 author는 제외
                .filter(userId -> !userId.equals(author.getId()))
                .collect(Collectors.toSet());

        // title
        String title = channel.getType().equals(ChannelType.PUBLIC)
                ? String.format("%s (#%s)", author.getUsername(), channel.getName())
                : author.getUsername();

        // 메시지 내용 (content)
        String content = message.getContent();

        // 해당 정보를 notificationService로 전송

    }
}
