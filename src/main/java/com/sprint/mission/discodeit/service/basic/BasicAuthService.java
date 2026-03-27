package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.LoginRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.InvalidPasswordException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicAuthService implements AuthService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto login(LoginRequest request) {
        log.debug("[AUTH_LOGIN] 로그인 시작: username={}", request.username());

        // 유저 검증, 없으면 예외 발생
        User user = validateAndGetUserByUsername(request.username());

        // 비밀번호 일치 검증
        validatePassword(request.password(), user.getPassword());

        // 유저 존재하면
        UserStatus userStatus = validateAndGetUserStatusByUserId(user.getId());

        // 온라인 상태 업데이트
        userStatus.setLastActiveAt(Instant.now());
        log.info("[AUTH_LOGIN_SUCCESS] 로그인 성공: userId={}", user.getId());

        return userMapper.toDto(user);
    }

    //// validation
    // user 객체 존재 확인
    private User validateAndGetUserByUsername(String username) {
        return userRepository.findByUsernameWithStatusAndProfile(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
    }

    private UserStatus validateAndGetUserStatusByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userStatusRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new UserStatusNotFoundException("userId", userId));
    }

    private void validatePassword(String requestPassword, String userPassword) {
        if (!userPassword.equals(requestPassword)) {
            throw new InvalidPasswordException();
        }
    }
}
