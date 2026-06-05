package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.notification.NotificationDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {

    Optional<NotificationDto> create(UUID receiverId, String title, String content);

    List<NotificationDto> findAllByReceiverId(UUID receiverId);

    void deleteByReceiverId(UUID receiverId, UUID notificationId);
}
