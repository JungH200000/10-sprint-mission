package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.dto.user.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.exception.user.DuplicatedEmailException;
import com.sprint.mission.discodeit.exception.user.DuplicatedUsernameException;
import com.sprint.mission.discodeit.exception.user.ProfileUploadFailedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BinaryContentStorage binaryContentStorage;

    @InjectMocks
    private BasicUserService basicUserService;

    @Nested
    @DisplayName("사용자 등록 테스트")
    class createUser {

        @Test
        @DisplayName("프로필 없는 사용자 등록에 성공해야 한다.")
        void success_create_user_without_profile() {
            // given(준비)
            UserCreateRequest request = new UserCreateRequest("test1@gmail.com", "test1", "1234");
            UserDto expectedUserDto = new UserDto(null, "test1", "test1@gmail.com", null, false);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userMapper.toDto(any(User.class))).thenReturn(expectedUserDto);

            // when(실행)
            UserDto result = basicUserService.create(request, null);

            // then(검증)
            assertEquals(expectedUserDto, result);

            verify(userRepository).existsByEmail(request.email());
            verify(userRepository).existsByUsername(request.username());

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(userRepository).save(any(User.class));
            verify(userMapper).toDto(any(User.class));
        }

        @Test
        @DisplayName("프로필 있는 사용자 등록에 성공해야 한다.")
        void success_create_user_with_profile() throws IOException {
            // given(준비)
            // UserCreateRequest -> request
            UserCreateRequest request = new UserCreateRequest("test1@gmail.com", "test1", "1234");

            // profile Mock
            MultipartFile profile = mock(MultipartFile.class);
            byte[] profileBytes = "test".getBytes();

            when(profile.isEmpty()).thenReturn(false);
            when(profile.getBytes()).thenReturn(profileBytes);
            when(profile.getOriginalFilename()).thenReturn("profile");
            when(profile.getContentType()).thenReturn("image/png");

            BinaryContentDto profileDto = new BinaryContentDto(null, profile.getOriginalFilename(), profile.getSize(), profile.getContentType());
            UserDto expectedUserDto = new UserDto(null, "test1", "test1@gmail.com", profileDto, false);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userMapper.toDto(any(User.class))).thenReturn(expectedUserDto);

            // when(실행)
            UserDto result = basicUserService.create(request, profile);

            // then(검증)
            assertEquals(expectedUserDto, result);

            verify(userRepository).existsByEmail(request.email());
            verify(userRepository).existsByUsername(request.username());

            verify(binaryContentRepository).save(any(BinaryContent.class));
            verify(binaryContentStorage).put(any(), eq(profileBytes));

            verify(userRepository).save(any(User.class));
            verify(userMapper).toDto(any(User.class));
        }

        @Test
        @DisplayName("이메일(email)이 중복되면 예외가 발생한다.")
        void fail_create_user_when_duplicated_email() {
            // given(준비)
            UserCreateRequest request = new UserCreateRequest("test1@gmail.com", "test1", "1234");

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // when(실행), then(검증)
            assertThrows(DuplicatedEmailException.class,
                    () -> basicUserService.create(request, null));

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자 이름(username)이 중복되면 예외가 발생한다.")
        void fail_create_user_when_duplicated_username() {
            // given(준비)
            UserCreateRequest request = new UserCreateRequest("test1@gmail.com", "test1", "1234");

            when(userRepository.existsByUsername(request.username())).thenReturn(true);

            // when(실행), then(검증)
            assertThrows(DuplicatedUsernameException.class,
                    () -> basicUserService.create(request, null));

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("프로필 업로드 실패하면 예외가 발생한다.")
        void fail_create_user_when_profile_upload() throws IOException {
            // given(준비)
            UserCreateRequest request = new UserCreateRequest("test1@gmail.com", "test1", "1234");
            MultipartFile profile = mock(MultipartFile.class);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUsername(request.username())).thenReturn(false);

            when(profile.isEmpty()).thenReturn(false);
            when(profile.getBytes()).thenThrow(new IOException("파일 읽기 실패"));

            // when(실행), then(검증)
            assertThrows(ProfileUploadFailedException.class,
                    () -> basicUserService.create(request, profile));

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("사용자 단건 조회")
    class findUser {

        @Test
        @DisplayName("사용자ID로 사용자 단건 조회를 할 수 있다.")
        void success_find_user() {
            // given(준비)
            UUID userId = UUID.randomUUID();
            User user = new User("test2@gmail.com", "test2", "1234", null);
            UserDto expectedUserDto = new UserDto(userId, "test2", "test2@gmail.com", null, false);

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(expectedUserDto);

            // when(실행)
            UserDto result = basicUserService.find(userId);

            // then(검증)
            assertEquals(expectedUserDto, result);

            verify(userRepository).findByIdWithStatusAndProfile(userId);
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외가 발생한다.")
        void fail_find_user_when_userId_null() {
            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicUserService.find(null));

            verify(userRepository, never()).findByIdWithStatusAndProfile(any());
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("해당 ID를 가진 사용자를 찾을 수 없으면 예외가 발생한다.")
        void fail_find_user_when_user_not_found() {
            // given(준비)
            UUID userId = UUID.randomUUID();

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.empty());

            // when(실행),  then(검증)
            assertThrows(UserNotFoundException.class,
                    () -> basicUserService.find(userId));

            verify(userRepository).findByIdWithStatusAndProfile(any());
            verify(userMapper, never()).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("사용자 목록 조회")
    class findAllUserList {

        @Test
        @DisplayName("사용자 목록을 조회할 수 있다.")
        void success_findAll_userList() {
            // given(준비)
            User user1 = new User("test3@gmail.com", "test3", "1234", null);
            User user2 = new User("test4@gmail.com", "test4", "1234", null);
            UserDto expectedUserDto1 = new UserDto(null, "test3", "test3@gmail.com", null, false);
            UserDto expectedUserDto2 = new UserDto(null, "test4", "test4@gmail.com", null, false);

            when(userRepository.findAllWithStatusAndProfile()).thenReturn(List.of(user1, user2));
            when(userMapper.toDto(user1)).thenReturn(expectedUserDto1);
            when(userMapper.toDto(user2)).thenReturn(expectedUserDto2);

            // when(실행)
            List<UserDto> result = basicUserService.findAll();

            // then(검증)
            assertEquals(2, result.size());
            assertEquals(expectedUserDto1, result.get(0));
            assertEquals(expectedUserDto2, result.get(1));

            verify(userRepository).findAllWithStatusAndProfile();
            verify(userMapper, times(2)).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자가 없을 때 사용자 목록 조회 시 빈 목록을 출력할 수 있다.")
        void success_findAll_userList_when_empty_userList() {
            // when(실행)
            List<UserDto> result = basicUserService.findAll();

            // then(검증)
            assertEquals(0, result.size());

            verify(userRepository).findAllWithStatusAndProfile();
            verify(userMapper, never()).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("사용자 정보 업데이트")
    class updateUser {

        @Test
        @DisplayName("사용자 ID로 프로필을 제외한 사용자 정보를 업데이트할 수 있다.")
        void success_update_user_without_profile() {
            // given(준비)
            User user = new User("test5@gmail.com", "test5", "1234", null);

            UUID userId = UUID.randomUUID();
            UserUpdateRequest request = new UserUpdateRequest("updateEmail@gmail.com", "12345", "updateUsername");
            UserDto expectedUserDto = new UserDto(userId, "updateUsername", "updateEmail@gmail.com", null, false);

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.of(user));
            when(userRepository.isEmailUsedByOther(userId, request.newEmail())).thenReturn(false);
            when(userRepository.isUserNameUsedByOther(userId, request.newUsername())).thenReturn(false);
            when(userMapper.toDto(user)).thenReturn(expectedUserDto);

            // when(실행)
            UserDto result = basicUserService.update(userId, request,null);

            // then(검증)
            assertEquals(expectedUserDto, result);

            verify(userRepository).findByIdWithStatusAndProfile(userId);
            verify(userRepository).isEmailUsedByOther(userId, request.newEmail());
            verify(userRepository).isUserNameUsedByOther(userId, request.newUsername());
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("사용자 ID로 모든 사용자 정보를 업데이트할 수 있다.")
        void success_update_user_with_profile() throws IOException {
            // given(준비)

            UUID oldProfileId = UUID.randomUUID();
            BinaryContent oldProfile = mock(BinaryContent.class);
            byte[] oldProfileBytes = "oldProfile".getBytes();

            when(oldProfile.getId()).thenReturn(oldProfileId);

            UUID userId = UUID.randomUUID();
            User user = new User("test5@gmail.com", "test5", "1234", oldProfile);

            UserUpdateRequest request = new UserUpdateRequest("updateEmail@gmail.com", "12345", "updateUsername");

            MultipartFile newProfileFile = mock(MultipartFile.class);
            byte[] newProfileBytes = "newProfile".getBytes();

            when(newProfileFile.isEmpty()).thenReturn(false);
            when(newProfileFile.getBytes()).thenReturn(newProfileBytes);
            when(newProfileFile.getOriginalFilename()).thenReturn("newProfile");
            when(newProfileFile.getContentType()).thenReturn("image/png");

            BinaryContentDto newProfileDto = new BinaryContentDto(null, newProfileFile.getOriginalFilename(), newProfileFile.getSize(), newProfileFile.getContentType());
            UserDto expectedUserDto = new UserDto(userId, "updateUsername", "updateEmail@gmail.com", newProfileDto, false);

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.of(user));
            when(binaryContentRepository.findById(oldProfileId)).thenReturn(Optional.of(oldProfile));
            when(binaryContentStorage.get(oldProfileId)).thenReturn(new ByteArrayInputStream(oldProfileBytes));
            when(userRepository.isUserNameUsedByOther(userId, request.newUsername())).thenReturn(false);
            when(userRepository.isEmailUsedByOther(userId, request.newEmail())).thenReturn(false);
            when(userMapper.toDto(user)).thenReturn(expectedUserDto);
            
            // when(실행)
            UserDto result = basicUserService.update(userId, request, newProfileFile);

            // then(검증)
            assertEquals(expectedUserDto, result);

            verify(userRepository).findByIdWithStatusAndProfile(userId);
            verify(userRepository).isUserNameUsedByOther(userId, request.newUsername());
            verify(userRepository).isEmailUsedByOther(userId, request.newEmail());

            verify(binaryContentRepository).findById(oldProfileId);
            verify(binaryContentStorage).get(oldProfileId);

            verify(binaryContentRepository).save(any(BinaryContent.class));
            verify(binaryContentStorage).put(any(), eq(newProfileBytes));

            verify(userMapper).toDto(user);
        }

        // user 객체 id = null 일 때
        // user 객체 not found 일 때
        // 기존 프로필 이미지 찾을 수 없음
        // 프로필 이미지 읽기 실패
        // 프로필 이미지 업로드 실패
        // 입력값 전부 null 아님
        // 기존 이메일과 새롭게 만든 게 이메일 중복
        // 기존 사용자 이름과 새롭게 만든 사용자 입력

        void update() {
            // given(준비)
            User user = new User("updateEmail@gmail.com", "updateUsername", "12345", null);
            // when(실행)

            // then(검증)
        }
    }


    @Nested
    @DisplayName("사용자 삭제")
    class deleteUser {

        @Test
        @DisplayName("사용자 ID로 사용자를 삭제할 수 있다")
        void success_delete_user() {
            // given(준비)
            UUID userId = UUID.randomUUID();
            User user = new User("test6@gmail.com", "test6", "1234", null);

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.of(user));

            // when(실행)
            basicUserService.delete(userId);

            // then(검증)
            verify(userRepository).findByIdWithStatusAndProfile(userId);
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외가 발생한다.")
        void delete_user_fail_when_userId_null() {
            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicUserService.delete(null));

            verify(userRepository, never()).findByIdWithStatusAndProfile(null);
            verify(userRepository, never()).deleteById(null);
        }

        @Test
        @DisplayName("해당 ID를 가진 사용자를 찾을 수 없으면 예외가 발생한다.")
        void delete_user_fail_when_user_not_found() {
            // given(준비)
            UUID userId = UUID.randomUUID();

            when(userRepository.findByIdWithStatusAndProfile(userId)).thenReturn(Optional.empty());

            // when(실행), then(검증)
            assertThrows(UserNotFoundException.class,
                    () -> basicUserService.delete(userId));

            verify(userRepository).findByIdWithStatusAndProfile(userId);
            verify(userRepository, never()).deleteById(userId);
        }
    }
}