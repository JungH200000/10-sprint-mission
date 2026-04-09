package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.LoginRequest;
import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.auth.LoginFailedException;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BasicAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusRepository userStatusRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private BasicAuthService basicAuthService;

    Instant now;
    Instant nowMinus10;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        nowMinus10 = now.minus(10, ChronoUnit.MINUTES);
    }

    private User createUser(String email, String username, String password, BinaryContent profile, Instant lastActiveAt) {
        User user = new User(email, username, password, profile);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        if (lastActiveAt != null) {
            new UserStatus(user, lastActiveAt);
        }

        return user;
    }

    private UserDto createUserDto(User user) {
        BinaryContent profile = user.getProfile() == null ? null : user.getProfile();
        BinaryContentDto profileDto = null;
        if (profile != null) {
            profileDto = new BinaryContentDto(profile.getId(), profile.getFileName(), profile.getSize(), profile.getContentType());
        }

        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), profileDto, true);
    }

    @Test
    @DisplayName("사용자 이름과 비밀번호로 로그인할 수 있다.")
    void success_login_with_username_and_password() {
        // given(준비)
        User existingUser = createUser("test@gmail.com", "test", "1234", null, nowMinus10);
        UserStatus existingUserStatus = existingUser.getStatus();

        LoginRequest request = new LoginRequest("test", "1234");
        UserDto expectedUserDto = createUserDto(existingUser);

        given(userRepository.findByUsernameWithStatusAndProfile(request.username())).willReturn(Optional.of(existingUser));
        given(userStatusRepository.findByUserIdWithUser(existingUser.getId())).willReturn(Optional.ofNullable(existingUserStatus));
        given(userMapper.toDto(existingUser)).willReturn(expectedUserDto);

        // when(실행)
        UserDto result = basicAuthService.login(request);

        // then(검증)
        assertEquals(expectedUserDto, result);
        assertEquals(expectedUserDto.id(), result.id());
        assertEquals(expectedUserDto.username(), result.username());

        verify(userRepository).findByUsernameWithStatusAndProfile(request.username());
        verify(userStatusRepository).findByUserIdWithUser(existingUser.getId());

        verify(userMapper).toDto(existingUser);
    }

    @Test
    @DisplayName("사용자 이름이 일치하지 않거나 존재하지 않으면 예외가 발생한다.")
    void fail_login_when_username() {
        // given(준비)
        User existingUser = createUser("test@gmail.com", "test", "1234", null, nowMinus10);

        LoginRequest request = new LoginRequest("testFail", "1234");

        given(userRepository.findByUsernameWithStatusAndProfile(request.username())).willThrow(new LoginFailedException());

        // when(실행), then(검증)
        assertThrows(LoginFailedException.class, () -> basicAuthService.login(request));

        verify(userRepository).findByUsernameWithStatusAndProfile(request.username());
        verify(userStatusRepository, never()).findByUserIdWithUser(existingUser.getId());

        verify(userMapper, never()).toDto(existingUser);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    void fail_login_when_password() {
        // given(준비)
        User existingUser = createUser("test@gmail.com", "test", "1234", null, nowMinus10);

        LoginRequest request = new LoginRequest("test", "testFail");

        given(userRepository.findByUsernameWithStatusAndProfile(request.username())).willReturn(Optional.ofNullable(any(User.class)));

        // when(실행), then(검증)
        assertThrows(LoginFailedException.class, () -> basicAuthService.login(request));

        verify(userRepository).findByUsernameWithStatusAndProfile(request.username());
        verify(userStatusRepository, never()).findByUserIdWithUser(existingUser.getId());

        verify(userMapper, never()).toDto(existingUser);
    }

    @Test
    @DisplayName("사용자 온라인 상태 정보가 존재하지 않으면 예외가 발생한다.")
    void fail_login_when_userStatus_not_found() {
        // given(준비)
        User existingUser = createUser("test@gmail.com", "test", "1234", null, nowMinus10);

        LoginRequest request = new LoginRequest("test", "1234");

        given(userRepository.findByUsernameWithStatusAndProfile(request.username())).willReturn(Optional.of(existingUser));
        given(userStatusRepository.findByUserIdWithUser(existingUser.getId())).willThrow(new UserStatusNotFoundException("userId", existingUser.getId()));

        // when(실행), then(검증)
        assertThrows(UserStatusNotFoundException.class, () -> basicAuthService.login(request));

        verify(userRepository).findByUsernameWithStatusAndProfile(request.username());
        verify(userStatusRepository).findByUserIdWithUser(existingUser.getId());

        verify(userMapper, never()).toDto(existingUser);
    }
}