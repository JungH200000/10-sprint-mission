package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionRegistry;
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

    private final SessionRegistry sessionRegistry;

    // 사용자 권한 수정
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserDto updateUserRole(UserRoleUpdateRequest request) {
        UUID userId = request.userId();
        Role newRole = request.newRole();

        log.debug("[USER_ROLE_UPDATE] 사용자 권한 수정 시작: userId={}, newRole={}",
                userId, newRole);

        User user = validateAndGetUserByUserIdWithStatusAndProfile(userId);
        Role oldRole = user.getRole();

        // 기존 Role과 요청 Role이 다르면
        if (!oldRole.equals(newRole)) {
            // 권한 수정
            user.updateRole(newRole);

            // 권한이 변경된 사용자의 로그인 세션 만료 처리
            expiredUserSession(userId);

            log.debug("[USER_ROLE_UPDATE] 사용자 권한 수정 완료: userId={}, role={}",
                    user.getId(), user.getRole());
        } else {
            log.debug("[USER_ROLE_UPDATE] 기존 권한과 요청 권한이 동일: userId={}, role={}",
                    userId, oldRole);
        }

        return userMapper.toDto(user);
    }

    // 권한이 변경된 사용자의 로그인 세션 만료 처리
    private void expiredUserSession(UUID userId) {
        sessionRegistry.getAllPrincipals().stream()
                // SessionRegistry에 등록된 principal 중 DiscodeitUserDetails만 필터링
                .filter(principal -> principal instanceof DiscodeitUserDetails)
                // DiscodeitUserDetails로 형변환
                .map(principal -> (DiscodeitUserDetails) principal)
                // 권한이 변경된 사용자와 동일한 userId를 가진 principal 찾기
                .filter(discodeitUserDetails -> discodeitUserDetails.getUserDto().id().equals(userId))
                // 찾은 principal의 만료되지 않은 세션 목록 조회 후 만료 처리
                .forEach(discodeitUserDetails -> {
                    sessionRegistry.getAllSessions(discodeitUserDetails, false)
                            .forEach(sessionInformation -> {
                                // 세션 만료 처리
                                sessionInformation.expireNow();

                                log.debug("[SESSION_EXPIRED] 권한 변경으로 세션 만료: userId={}, sessionId={}",
                                        userId, sessionInformation.getSessionId());
                            });
                });
    }

    // validation
    // 사용자 존재 확인
    private User validateAndGetUserByUserIdWithStatusAndProfile(UUID userID) {
        return userRepository.findByIdWithStatusAndProfile(userID)
                .orElseThrow(() -> new UserNotFoundException("userId", userID));
    }
}
