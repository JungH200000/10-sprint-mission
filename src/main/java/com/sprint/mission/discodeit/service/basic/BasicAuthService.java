package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.LoginRequest;
import com.sprint.mission.discodeit.dto.user.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicAuthService implements AuthService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

    @Override
    public UserDto login(LoginRequest loginRequest) {
        // 유저 검증, 없으면 예외 발생
        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new NoSuchElementException("User with username " + loginRequest.username() + " not found."));
        if (!user.getPassword().equals(loginRequest.password())) {
            throw new IllegalArgumentException("Wrong password");
        }

        // 유저 존재하면
        UserStatus userStatus = userStatusRepository.findByUserIdWithUser(user.getId())
                .orElseThrow(() -> new NoSuchElementException("UserStatus with id " + user.getId() + " not found."));

        // 온라인 상태 업데이트
        userStatus.setLastActiveAt(Instant.now());
        userStatusRepository.save(userStatus);

        return createUserInfo(user);
    }

    private UserDto createUserInfo(User user) {
        return new UserDto(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getEmail(),
                user.getUsername(),
                user.getProfile(),
                user.getStatus().isOnlineStatus());
    }
}
