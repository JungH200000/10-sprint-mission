package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.user.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.user.response.UserDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Validated
public class BasicUserService implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public User createUser(UserCreateRequest request, MultipartFile profile) {
        // newEmail, newUsername 중복 확인
        validateDuplicateEmail(request.email());
        validateDuplicateUserName(request.username());

        User user = new User(request.email(), request.username(), request.password(), request.birthday());

        if (profile != null && !profile.isEmpty()) {
            try {
                byte[] bytes = profile.getBytes();
                if (isBinaryContent(bytes)) {
                    BinaryContent binaryContent = new BinaryContent(
                            profile.getOriginalFilename(),
                            profile.getContentType(),
                            bytes,
                            (long) bytes.length
                    );
                    binaryContentRepository.save(binaryContent);
                    user.updateProfileId(binaryContent.getId());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("profileImage 업로드 실패", e);
            }
        }
        UserStatus userStatus = new UserStatus(user.getId());

        userRepository.save(user);
        userStatusRepository.save(userStatus);
        return user;
    }

    @Override
    public UserDto findUserById(UUID userId) {
        // User ID null 검증
        ValidationMethods.validateId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다."));
        UserStatus userStatus = userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 userId에 맞는 UserStatus가 없습니다."));

        return createUserWithOnlineResponse(user, userStatus);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<UserStatus> userStatuses = userStatusRepository.findAll();

        List<UserDto> userInfos = new ArrayList<>();
        userStatuses.forEach(status -> {
            User user = userRepository.findById(status.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("status의 userId를 가진 유저가 존재하지 않음"));
            userInfos.add(createUserWithOnlineResponse(user, status));
        });

        return userInfos;
    }

    @Override
    public User updateUser(UUID userId, UserUpdateRequest userUpdateRequest, MultipartFile profile) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));

        // 새로운 BinaryContent가 들어왔다면 true / 들어왔는데 기존과 동일하다면 false / 안들어왔다면 false
        byte[] bytes = null;
        boolean binaryContentChanged = false;
        if (profile != null && !profile.isEmpty()) { // 새 BinaryContent 들어오는데
            try {
                bytes = profile.getBytes();
                binaryContentChanged = isBinaryContentChanged(bytes, user.getProfileId());
            } catch (IOException e) {
                throw new IllegalArgumentException("profileImage 업로드 실패", e);
            }
        }

        // newEmail or newPassword or newUsername 등이 "전부" 입력되지 않았거나 "전부" 이전과 동일하다면 exception 발생시킴
        validateAllInputDuplicateOrEmpty(userUpdateRequest, user, binaryContentChanged);

        // 다른 사용자들의 email과 중복되는지 확인 후 newEmail 업데이트
        if (userUpdateRequest.newEmail() != null && !user.getEmail().equals(userUpdateRequest.newEmail())) {
            validateDuplicateEmailForUpdate(userId, userUpdateRequest.newEmail());
            user.updateEmail(userUpdateRequest.newEmail());
        }
        // 다른 사용자들의 userName과 중복되는지 확인 후 newUsername 업데이트
        if (userUpdateRequest.newUsername() != null && !user.getUsername().equals(userUpdateRequest.newUsername())) {
            validateDuplicateUserNameForUpdate(userId, userUpdateRequest.newUsername());
            user.updateUserName(userUpdateRequest.newUsername());
        }

        // filter로 중복 확인 후 업데이트(중복 확인 안하면 동일한 값을 또 업데이트함)
        Optional.ofNullable(userUpdateRequest.newPassword())
                .filter(p -> !user.getPassword().equals(p)) // !false(중복 아닌 값) -> true
                .ifPresent(p -> user.updatePassword(p));
        Optional.ofNullable(userUpdateRequest.newBirthday())
                .filter(b -> !user.getBirthday().equals(b))
                .ifPresent(b -> user.updateBirthday(b));

        UUID oldProfileId = user.getProfileId();
        if (binaryContentChanged) {
            BinaryContent newBinaryContent = new BinaryContent(
                    profile.getOriginalFilename(),
                    profile.getContentType(),
                    bytes,
                    (long) bytes.length
            );
            binaryContentRepository.save(newBinaryContent);
            user.updateProfileId(newBinaryContent.getId());
        }
        userRepository.save(user);

        // BinaryContent가 교체되고, profileId가 null이 아닐 때
        if (binaryContentChanged && oldProfileId != null) {
            binaryContentRepository.delete(oldProfileId);
        }

        return user;
    }

    @Override
    public void deleteUser(UUID userId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);

        UserStatus userStatus = userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus with userId " + userId + " not found"));

        // channel, message 삭제는 상위에서
        if (user.getProfileId() != null) {
            binaryContentRepository.delete(user.getProfileId());
        }
        userStatusRepository.deleteByUserId(userId);
        userRepository.delete(userId);
    }

    private UserDto createUserWithOnlineResponse(User user, UserStatus userStatus) {
        return new UserDto(
                user.getId(), user.getCreatedAt(), user.getUpdatedAt(),
                user.getEmail(), user.getUsername(), user.getBirthday(),
                user.getProfileId(), userStatus.isOnlineStatus());
    }

    //// validation
    // user ID null & user 객체 존재 확인
    public User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found."));
    }
    // email이 이미 존재하는지 확인
    private void validateDuplicateEmail(String newEmail) {
        if (userRepository.existEmail(newEmail)) {
            throw new IllegalArgumentException("user with newEmail " + newEmail + " already exists");
        }
    }
    // userName이 이미 존재하는지 확인
    private void validateDuplicateUserName(String newUserName) {
        if (userRepository.existUserName(newUserName)) {
            throw new IllegalArgumentException("user with userName이 " + newUserName + " already exists");
        }
    }
    // newEmail or newPassword or newUsername 등이 "전부" 입력되지 않았거나 "전부" 이전과 동일하다면 exception 발생시킴
    private void validateAllInputDuplicateOrEmpty(UserUpdateRequest request, User user, boolean binaryContentChanged) {
        if ((request.newEmail() == null || user.getEmail().equals(request.newEmail()))
                && (request.newPassword() == null || user.getPassword().equals(request.newPassword()))
                && (request.newUsername() == null || user.getUsername().equals(request.newUsername()))
                && (request.newBirthday() == null || user.getBirthday().equals(request.newBirthday()))
                && !binaryContentChanged
        ) {
            throw new IllegalArgumentException("변경사항이 없습니다. 입력 값을 다시 확인하세요.");
        }
    }
    // 새로운 BinaryContent가 들어왔다면 true / 들어왔는데 기존과 동일하다면 false / 안들어왔다면 false
    private boolean isBinaryContentChanged (byte[] bytes, UUID profileId) {
        if (profileId == null) { // 기존에 BinaryContent 없을 때
            return true; // 새로운 BinaryContent 들어옴
        } else { //기존 프로필이 존재
            BinaryContent oldBinaryContent = binaryContentRepository.findById(profileId)
                    .orElseThrow(() -> new NoSuchElementException("해당 profileId에 해당하는 BinaryContent가 없습니다."));
            // 새로 들어온 BinaryContent와 비교
            // 같으면 -> false -> change 되지 않음
            return !Arrays.equals(oldBinaryContent.getBytes(), bytes);
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
