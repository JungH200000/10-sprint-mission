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
@Slf4j
@Transactional
public class BasicUserStatusService implements UserStatusService {
    private final UserStatusRepository userStatusRepository;
    private final UserRepository userRepository;
    private final UserStatusMapper userStatusMapper;

    @Override
    public UserStatusDto create(UserStatusCreateRequest request) {
        log.debug("[USER_STATUS_CREATE] 사용자 온라인 상태 생성 시작: userId={}", request.userId());

        User user = userRepository.findByIdWithStatusAndProfile(request.userId())
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다."));

        if (userStatusRepository.existsUserStatusByUserId(user.getId())) {
            throw new IllegalArgumentException("이미 존재하는 UserStatus가 있습니다.");
        }

        UserStatus userStatus = new UserStatus(user, Instant.now());
        userStatusRepository.save(userStatus);
        log.info("[USER_STATUS_CREATE] 사용자 온라인 상태 생성 완료: userStatusID={}, userId={}, lastActiveAt={}, isOnline={}", userStatus.getId(), userStatus.getUser().getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public UserStatusDto find(UUID userStatusId) {
        log.debug("[USER_STATUS_FIND] 사용자 온라인 상태 조회 시작: userStatusId={}", userStatusId);

        UserStatus userStatus = validateAndGetUserStatusByUserStatusId(userStatusId);
        log.debug("[USER_STATUS_FIND] 사용자 온라인 상태 조회 완료: userStatusID={}, userId={}, lastActiveAt={}, isOnline={}", userStatus.getId(), userStatus.getUser().getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public UserStatusDto findByUserId(UUID userId) {
        log.debug("[USER_STATUS_FIND_BY_USERID] userId로 사용자 온라인 상태 조회 시작: userId={}", userId);

        UserStatus userStatus = validateAndGetUserStatusByUserId(userId);
        log.debug("[USER_STATUS_FIND_BY_USERID] userId로 사용자 온라인 상태 조회 완료: userStatusID={}, userId={}, lastActiveAt={}, isOnline={}", userStatus.getId(), userStatus.getUser().getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserStatusDto> findAll() {
        log.debug("[USER_STATUS_LIST_FIND] 사용자 온라인 상태 목록 조회 시작");

        List<UserStatusDto> userStatusDtoList = userStatusRepository.findAllWithUser().stream()
                .map(userStatus -> userStatusMapper.toDto(userStatus))
                .toList();
        log.debug("[USER_STATUS_LIST_FIND] 사용자 온라인 상태 목록 조회 완료: count={}", userStatusDtoList.size());

        return userStatusDtoList;
    }

    @Override
    public UserStatusDto update(UUID userStatusId, UserStatusUpdateRequest request) {
        log.debug("[USER_STATUS_UPDATE] 사용자 온라인 상태 수정 시작: userStatusId={}, newLastActiveAt={}", userStatusId, request.newLastActiveAt());

        if (request.newLastActiveAt() == null) {
            throw new IllegalArgumentException("newLastActiveAt null로 입력되었습니다.");
        }

        UserStatus userStatus = validateAndGetUserStatusByUserStatusId(userStatusId);

        userStatus.setLastActiveAt(request.newLastActiveAt());
        userStatusRepository.save(userStatus);
        log.info("[USER_STATUS_UPDATE] 사용자 온라인 상태 수정 완료: userStatusID={}, userId={}, lastActiveAt={}, isOnline={}", userStatus.getId(), userStatus.getUser().getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Override
    public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request) {
        log.debug("[USER_STATUS_UPDATE_BY_USERID] userId로 사용자 온라인 상태 수정 시작: userId={}, newLastActiveAt={}", userId, request.newLastActiveAt());

        if (request.newLastActiveAt() == null) {
            throw new IllegalArgumentException("newLastActiveAt null로 입력되었습니다.");
        }
        validateAndGetUserByUserId(userId);
        UserStatus userStatus = validateAndGetUserStatusByUserId(userId);

        userStatus.setLastActiveAt(request.newLastActiveAt());
        userStatusRepository.save(userStatus);
        log.info("[USER_STATUS_UPDATE_BY_USERID] userId로 사용자 온라인 상태 수정 완료: userStatusID={}, userId={}, lastActiveAt={}, isOnline={}", userStatus.getId(), userStatus.getUser().getId(), userStatus.getLastActiveAt(), userStatus.isOnlineStatus());

        return userStatusMapper.toDto(userStatus);
    }

    @Override
    public void delete(UUID userStatusId) {
        log.debug("[USER_STATUS_DELETE] 사용자 온라인 상태 삭제 시작: userStatusId={}", userStatusId);

        validateAndGetUserStatusByUserStatusId(userStatusId);
        userStatusRepository.deleteById(userStatusId);
        log.info("[USER_STATUS_DELETE] 사용자 온라인 상태 삭제 완료: userStatusId={}", userStatusId);
    }

    /// / validation
    // user ID null & user 객체 존재 확인
    private void validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }

    private UserStatus validateAndGetUserStatusByUserStatusId(UUID userStatusId) {
        ValidationMethods.validateId(userStatusId);
        return userStatusRepository.findByIdWithUser(userStatusId)
                .orElseThrow(() -> new NoSuchElementException("해당 UserStatus가 없습니다."));
    }

    private UserStatus validateAndGetUserStatusByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userStatusRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus with id " + userId + " not found."));
    }
}
