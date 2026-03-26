package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.userstatus.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserStatusService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicUserStatusService implements UserStatusService {
    private final UserStatusRepository userStatusRepository;
    private final UserRepository userRepository;
    private final UserStatusMapper userStatusMapper;

    @Override
    public UserStatusDto create(UserStatusCreateRequest request) {
        User user = userRepository.findByIdWithStatusAndProfile(request.userId())
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다."));

        if (userStatusRepository.existsUserStatusByUserId(user.getId())) {
            throw new IllegalArgumentException("이미 존재하는 UserStatus가 있습니다.");
        }

        UserStatus userStatus = new UserStatus(user, Instant.now());
        userStatusRepository.save(userStatus);

        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public UserStatusDto find(UUID userStatusId) {
        UserStatus userStatus = validateAndGetUserStatusByUserStatusId(userStatusId);
        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public UserStatusDto findByUserId(UUID userId) {
        UserStatus userStatus = validateAndGetUserStatusByUserId(userId);
        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserStatusDto> findAll() {
        return userStatusRepository.findAllWithUser().stream()
                .map(userStatus -> userStatusMapper.toDto(userStatus))
                .toList();
    }

    @Override
    public UserStatusDto update(UUID userStatusId, UserStatusUpdateRequest request) {
        if (request.newLastActiveAt() == null) {
            throw new IllegalArgumentException("newLastActiveAt null로 입력되었습니다.");
        }

        UserStatus userStatus = validateAndGetUserStatusByUserStatusId(userStatusId);

        userStatus.setLastActiveAt(request.newLastActiveAt());
        userStatusRepository.save(userStatus);

        return userStatusMapper.toDto(userStatus);
    }

    @Override
    public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest userStatusUpdateRequest) {
        log.info("[USER_UPDATE_ONLINE_STATUS] 사용자 온라인 상태 업데이트 시작: userId={}, newLastActiveAt={}", userId, userStatusUpdateRequest.newLastActiveAt());

        if (userStatusUpdateRequest.newLastActiveAt() == null) {
            throw new IllegalArgumentException("newLastActiveAt null로 입력되었습니다.");
        }
        validateUserByUserId(userId);
        UserStatus userStatus = validateAndGetUserStatusByUserId(userId);

        userStatus.setLastActiveAt(userStatusUpdateRequest.newLastActiveAt());
        userStatusRepository.save(userStatus);
        log.info("[USER_UPDATE_ONLINE_STATUS] 사용자 온라인 상태 업데이트 완료: userId={}, userStatusID={}, lastActiveAt={}, isOnline={}", userStatus.getUser().getId(), userStatus.getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Override
    public void delete(UUID userStatusId) {
        validateUserStatusByUserStatusId(userStatusId);
        userStatusRepository.deleteById(userStatusId);
    }

    //// validation
    // user ID null & user 객체 존재 확인
    public void validateUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다."));
    }

    public UserStatus validateAndGetUserStatusByUserStatusId(UUID userStatusId) {
        ValidationMethods.validateId(userStatusId);
        return userStatusRepository.findByIdWithUser(userStatusId)
                .orElseThrow(() -> new NoSuchElementException("해당 UserStatus가 없습니다."));
    }
    public void validateUserStatusByUserStatusId(UUID userStatusId) {
        ValidationMethods.validateId(userStatusId);
        userStatusRepository.findByIdWithUser(userStatusId)
                .orElseThrow(() -> new NoSuchElementException("해당 UserStatus가 없습니다."));
    }

    public UserStatus validateAndGetUserStatusByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userStatusRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus with id " + userId + " not found."));
    }
}
