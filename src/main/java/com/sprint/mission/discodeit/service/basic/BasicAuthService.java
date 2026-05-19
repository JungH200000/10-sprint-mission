package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // 사용자 권한 수정
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserDto updateUserRole(UserRoleUpdateRequest request) {
        UUID userId = request.userId();
        Role newRole = request.newRole();

        log.debug("[USER_ROLE_UPDATE] 사용자 권한 수정 시작: userId={}, newRole={}",
                userId, newRole);

        User user = validateAndGetUserByUserIdWithStatusAndProfile(userId);

        // 권한 수정
        user.updateRole(newRole);

        log.debug("[USER_ROLE_UPDATE] 사용자 권한 수정 완료: userId={}, role={}",
                user.getId(), user.getRole());

        return userMapper.toDto(user);
    }

    // validation
    // 사용자 존재 확인
    private User validateAndGetUserByUserIdWithStatusAndProfile(UUID userID) {
        return userRepository.findByIdWithStatusAndProfile(userID)
                .orElseThrow(() -> new UserNotFoundException("userId", userID));
    }
}
