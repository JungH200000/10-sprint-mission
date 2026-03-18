package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.user.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicUserService implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final UserMapper userMapper;
    private final BinaryContentStorage binaryContentStorage;

    @Override
    public UserDto create(UserCreateRequest request, MultipartFile profile) {
        // newEmail, newUsername 중복 확인
        validateDuplicateEmail(request.email());
        validateDuplicateUserName(request.username());

        BinaryContent binaryContent = null;
        byte[] bytes = null;
        if (profile != null && !profile.isEmpty()) {
            try {
                bytes = profile.getBytes();
                binaryContent = new BinaryContent(
                        profile.getOriginalFilename(),
                        profile.getContentType(),
                        (long) bytes.length
                );
                binaryContentRepository.save(binaryContent); // 없으면 UUID가 생성 안됨
                binaryContentStorage.put(binaryContent.getId(), bytes);
            } catch (IOException e) {
                throw new IllegalArgumentException("profileImage 업로드 실패", e);
            }
        }
        User user = new User(request.email(), request.username(), request.password(), binaryContent);
        UserStatus userStatus = new UserStatus(user, Instant.now());

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto find(UUID userId) {
        // User ID null 검증
        User user = validateAndGetUserByUserId(userId);

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findAll() {
        return userRepository.findAllWithStatusAndProfile().stream()
                .map(user -> userMapper.toDto(user))
                .toList();
    }

    @Override
    public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest, MultipartFile profile) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);

        // 새로운 BinaryContent가 들어왔다면 true / 들어왔는데 기존과 동일하다면 false / 안들어왔다면 false
        byte[] bytes = null;
        boolean binaryContentChanged = false;
        if (profile != null && !profile.isEmpty()) {
            try {
                bytes = profile.getBytes();
                binaryContentChanged = isProfileChanged(bytes, user.getProfile());
            } catch (IOException e) {
                throw new IllegalArgumentException("profileImage 업로드 실패", e);
            }
        }

        // newEmail or newPassword or newUsername 등이 "전부" 입력되지 않았거나 "전부" 이전과 동일하다면 exception 발생시킴
        validateAllInputDuplicateOrEmpty(userUpdateRequest, user, binaryContentChanged);

        String newEmail = userUpdateRequest.newEmail();
        String newUsername = userUpdateRequest.newUsername();
        String newPassword = userUpdateRequest.newPassword();

        // filter로 중복 확인 후 업데이트(중복 확인 안하면 동일한 값을 또 업데이트함)
        Optional.ofNullable(newEmail)
                .filter(e -> !user.getEmail().equals(e))
                .ifPresent(e -> {
                    // 다른 사용자들의 email과 중복되는지 확인 후 newEmail 업데이트
                    validateDuplicateEmailForUpdate(userId, e);
                    user.setEmail(e);
                });
        Optional.ofNullable(newUsername)
                .filter(n -> !user.getUsername().equals(n))
                .ifPresent(n -> {
                    // 다른 사용자들의 userName과 중복되는지 확인 후 newUsername 업데이트
                    validateDuplicateUserNameForUpdate(userId, n);
                    user.setUsername(n);
                });
        Optional.ofNullable(newPassword)
                .filter(p -> !user.getPassword().equals(p)) // !false(중복 아닌 값) -> true
                .ifPresent(p -> user.setPassword(p));

        if (binaryContentChanged) {
            BinaryContent newProfile = new BinaryContent(
                    profile.getOriginalFilename(),
                    profile.getContentType(),
                    (long) bytes.length
            );
            binaryContentRepository.save(newProfile); // 없으면 UUID가 생성 안됨
            binaryContentStorage.put(newProfile.getId(), bytes);
            user.setProfile(newProfile);
        }
//        userRepository.save(user);

        return userMapper.toDto(user);
    }

    @Override
    public void delete(UUID userId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        validateUserByUserId(userId);

        userRepository.deleteById(userId);
    }

    //// validation
    // user ID null & user 객체 존재 확인
    public User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findByIdWithStatusAndProfile(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public void validateUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    // userStatus ID null & userStatus 객체 존재 확인
    public UserStatus validateAndGetUserStatusByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userStatusRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus with userId " + userId + " not found."));
    }

    // email이 이미 존재하는지 확인
    private void validateDuplicateEmail(String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("user with newEmail " + newEmail + " already exists");
        }
    }
    // userName이 이미 존재하는지 확인
    private void validateDuplicateUserName(String newUserName) {
        if (userRepository.existsByUsername(newUserName)) {
            throw new IllegalArgumentException("user with userName이 " + newUserName + " already exists");
        }
    }

    // newEmail or newPassword or newUsername 등이 "전부" 입력되지 않았거나 "전부" 이전과 동일하다면 exception 발생시킴
    private void validateAllInputDuplicateOrEmpty(UserUpdateRequest request, User user, boolean binaryContentChanged) {
        if ((request.newEmail() == null || user.getEmail().equals(request.newEmail()))
                && (request.newPassword() == null || user.getPassword().equals(request.newPassword()))
                && (request.newUsername() == null || user.getUsername().equals(request.newUsername()))
                && !binaryContentChanged
        ) {
            throw new IllegalArgumentException("변경사항이 없습니다. 입력 값을 다시 확인하세요.");
        }
    }

    // 새로운 BinaryContent가 들어왔다면 true / 들어왔는데 기존과 동일하다면 false / 안들어왔다면 false
    private boolean isProfileChanged(byte[] bytes, BinaryContent profile) {
        if (profile == null) { // 기존에 BinaryContent 없을 때
            return true; // 새로운 BinaryContent 들어옴
        }
        //기존 프로필이 존재
        BinaryContent oldBinaryContent = binaryContentRepository.findById(profile.getId())
                .orElseThrow(() -> new NoSuchElementException("해당 profileId에 해당하는 BinaryContent가 없습니다."));
        // 새로 들어온 BinaryContent와 비교
        // 같으면 -> false -> change 되지 않음
        try {
            byte[] oldBytes = binaryContentStorage.get(oldBinaryContent.getId()).readAllBytes();
            return !Arrays.equals(oldBytes, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("기존 프로필 데이터를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    // binaryContent가 null 인지, 없는지 확인
    public boolean isBinaryContent(byte[] bytes) {
        return bytes != null && bytes.length != 0;
    }
    // 나를 제외한 newEmail 중에 중복된 값이 있는지 확인
    private void validateDuplicateEmailForUpdate(UUID userId, String newEmail) {
        if (userRepository.isEmailUsedByOther(userId, newEmail)) {
            throw new IllegalArgumentException("user with newEmail " + newEmail + " already exists");
        }
    }

    // 나를 제외한 newUsername 중에 중복된 값이 있는지 확인
    private void validateDuplicateUserNameForUpdate(UUID userId, String newUserName) {
        if (userRepository.isUserNameUsedByOther(userId, newUserName)) {
            throw new IllegalArgumentException("user with newUsername " + newUserName + " already exists");
        }
    }
}
