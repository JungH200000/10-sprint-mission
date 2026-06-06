package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// MessageCreatedEvent를 받아 메시지 생성 시 알림을 발행하는 Listener
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRequiredEventListener {

    private final NotificationService notificationService;
    private final ReadStatusRepository readStatusRepository;

    // 채널에 새로운 메시지 생성 시 알림을 설정한 모든 참가자에게 알림을 보내는 Listener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MessageCreatedEvent event) {
        Message message = event.getMessage();
        Channel channel = message.getChannel();
        User author = message.getAuthor();

        // 채널 알림 여부를 활성화(true)한 ReadStatus 조회한 후 사용자 ID Set(중복 방지)
        Set<UUID> receiverIds = readStatusRepository
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

        // 해당 정보를 notificationService로 전송해 알림 생성
        notificationService.create(receiverIds, title, content);
    }

    // 권한(role)이 변경된 사용자에게 알림을 보내는 Listener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleUpdatedEvent event) {
        UUID userId = event.getUserId();
        Role oldRole = event.getOldRole();
        Role newRole = event.getNewRole();

        String title = "권한이 변경되었습니다.";
        String content = String.format("%s -> %s", oldRole, newRole);

        notificationService.create(Set.of(userId), title, content);
    }
}
