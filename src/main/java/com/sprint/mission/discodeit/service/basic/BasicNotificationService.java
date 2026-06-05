package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.notification.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BasicNotificationService implements NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Optional<NotificationDto> create(
            UUID receiverId,
            String title,
            String content
    ) {

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationDto> findAllByReceiverId(UUID receiverId) {
        log.debug("[NOTIFICATION_LIST_FIND] 알림 목록 조회 시작: receiverId={}", receiverId);

        // 사용자(receiver) 검증
        validateAndGetUserByUserId(receiverId);
        
        List<NotificationDto> notificationDtoList = notificationRepository
                .findAllByReceiverIdOrderByCreatedAtDesc(receiverId)
                .stream()
                .map(notification -> notificationMapper.toDto(notification))
                .toList();
        
        log.debug("[NOTIFICATION_LIST_FIND] 알림 목록 조회 완료: count={}", notificationDtoList.size());

        return notificationDtoList;
    }

    @PreAuthorize("#receiverId != null and #receiverId.equals(authentication.principal.userDto.id)")
    @Override
    public void deleteByReceiverId(UUID receiverId, UUID notificationId) {
        log.debug("[NOTIFICATION_CHECK] 알림 확인 시작: receiverId={}, notificationId={}",
                receiverId, notificationId);

        // 사용자(receiver) 검증
        validateAndGetUserByUserId(receiverId);

        // 알림 검증
        Notification notification = validateAndGetNotificationByUserId(notificationId);

        // 알림 확인(삭제)
        notificationRepository.delete(notification);

        log.debug("[NOTIFICATION_CHECK] 알림 확인 완료: receiverId={}, notificationId={}",
                receiverId, notificationId);
    }

    // 사용자(receiver) 검증
    private User validateAndGetUserByUserId(UUID userId) {
        if (userId == null) {
            throw new InvalidInputException("userId", null);
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("userId", userId));
    }

    // 알림 검증
    private Notification validateAndGetNotificationByUserId(UUID notificationId) {
        if (notificationId == null) {
            throw new InvalidInputException("notificationId", null);
        }

        return notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new NotificationNotFoundException("notificationId", notificationId)
                );
    }
}
