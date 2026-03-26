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
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicUserService implements UserService {
    private final UserRepository userRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final UserMapper userMapper;
    private final BinaryContentStorage binaryContentStorage;

    @Override
    public UserDto create(UserCreateRequest request, MultipartFile profile) {
        log.debug("[USER_CREATE] 사용자 등록 시작: email={}, username={}", request.email(), request.username());

        // newEmail, newUsername 중복 확인
        validateDuplicateEmail(request.email());
        validateDuplicateUserName(request.username());

        BinaryContent binaryContent = null;
        byte[] bytes;
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

                log.info("[PROFILE_SAVE] 프로필 저장 완료: profileID={}, type={}, size={}", binaryContent.getId(), binaryContent.getContentType(), binaryContent.getSize());

            } catch (IOException e) {
                throw new IllegalArgumentException("profileImage 업로드 실패", e);
            }
        }
        User user = new User(request.email(), request.username(), request.password(), binaryContent);
        new UserStatus(user, Instant.now());

        userRepository.save(user);
        log.info("[USER_CREATE] 사용자 등록 완료: userId={}, email={}, username={}, profileId={}", user.getId(), user.getEmail(), user.getUsername(), user.getProfile() != null ? user.getProfile().getId() : null);

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto find(UUID userId) {
        log.debug("[USER_FIND] 사용자 조회 시작"); // 단순 조회 -> DEBUG

        // User ID null 검증
        User user = validateAndGetUserByUserId(userId);
        log.debug("[USER_FIND] 사용자 조회 완료: userId={}, email={}, username={}, profileId={}", user.getId(), user.getEmail(), user.getUsername(), user.getProfile() != null ? user.getProfile().getId() : null);

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findAll() {
        log.debug("[USER_LIST_FIND] 사용자 목록 조회 시작"); // 단순 조회 -> DEBUG

        List<UserDto> userDtoList = userRepository.findAllWithStatusAndProfile().stream()
                .map(user -> userMapper.toDto(user))
                .toList();
        log.debug("[USER_LIST_FIND] 사용자 목록 조회 완료: size={}", userDtoList.size());

        return userDtoList;
    }

    @Override
    public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest, MultipartFile profile) {
        log.debug("[USER_UPDATE] 사용자 정보 업데이트 시작: userId={}, isInputNewEmail={}, isInputNewUsername={}, isInputNewPassword={}", userId, userUpdateRequest.newEmail() != null, userUpdateRequest.newUsername() != null, userUpdateRequest.newPassword() != null);

        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);

        // 입력값과 현재 값을 비교해서 같으면 null, 새롭게 입력된 값이면 입력값
        String newEmail = changedString(userUpdateRequest.newEmail(), user.getEmail());
        String newUsername = changedString(userUpdateRequest.newUsername(), user.getUsername());
        String newPassword = changedString(userUpdateRequest.newPassword(), user.getPassword());

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
        log.info("[USER_UPDATE] 기존 사용자 정보에 따른 입력값 중복/변경 상태: isDuplicatedNewEmail={}, isDuplicatedNewUsername={}, isDuplicatedNewPassword={}, isChangedProfile={}", newEmail == null, newUsername == null, newPassword == null, binaryContentChanged);

        // 전부 입력 X이거나 전부 현재 값과 동일(전부 null)할 때 검증
        validateAllRequestExistingOrNull(newEmail, newUsername, newPassword, binaryContentChanged);

        // 다른 사용자들과 중복 확인
        validateDuplicateUsernameForUpdate(userId, newUsername);
        validateDuplicateEmailForUpdate(userId, newEmail);

        BinaryContent newProfile = null;
        if (binaryContentChanged) {
            newProfile = new BinaryContent(
                    profile.getOriginalFilename(),
                    profile.getContentType(),
                    (long) bytes.length
            );
            binaryContentRepository.save(newProfile); // 없으면 UUID가 생성 안됨
            binaryContentStorage.put(newProfile.getId(), bytes);

            log.info("[PROFILE_SAVE] 프로필 저장 완료: profileID={}, type={}, size={}", newProfile.getId(), newProfile.getContentType(), newProfile.getSize());
        }

        user.update(newUsername, newEmail, newPassword, newProfile);

        log.info("[USER_UPDATE] 사용자 정보 업데이트 완료: userId={}, email={}, username={}, profileId={}", user.getId(), user.getEmail(), user.getUsername(), user.getProfile() != null ? user.getProfile().getId() : null);

        return userMapper.toDto(user);
    }

    @Override
    public void delete(UUID userId) {
        log.debug("[USER_DELETE] 사용자 삭제 시작: userId={}", userId);

        // 로그인 되어있는 user ID null / user 객체 존재 확인
        validateAndGetUserByUserId(userId);

        userRepository.deleteById(userId);
        log.info("[USER_DELETE] 사용자 삭제 완료: userId={}", userId);
    }

    //// validation
    // user ID null & user 객체 존재 확인
    private User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findByIdWithStatusAndProfile(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }

    // email이 이미 존재하는지 확인
    private void validateDuplicateEmail(String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("user with newEmail " + newEmail + " already exists");
        }
    }

    // userName이 이미 존재하는지 확인
    private void validateDuplicateUserName(String newUsername) {
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("user with userName " + newUsername + " already exists");
        }
    }

    // 입력값과 현재 값을 비교해서 같으면 null, 새롭게 입력된 값이면 입력값
    private String changedString(String requestValue, String userValue) {
        return requestValue != null && !requestValue.equals(userValue)
                ? requestValue
                : null;
    }

    // 전부 입력 X이거나 전부 현재 값과 동일(전부 null)할 때 검증
    private void validateAllRequestExistingOrNull(String newEmail, String newUsername, String password, boolean binaryContentChanged) {
        if (newEmail == null
                && newUsername == null
                && password == null
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

    // 나를 제외한 newEmail 중에 중복된 값이 있는지 확인
    private void validateDuplicateEmailForUpdate(UUID userId, String newEmail) {
        if (userRepository.isEmailUsedByOther(userId, newEmail)) {
            throw new IllegalArgumentException("user with newEmail " + newEmail + " already exists");
        }
    }

    // 나를 제외한 newUsername 중에 중복된 값이 있는지 확인
    private void validateDuplicateUsernameForUpdate(UUID userId, String newUsername) {
        if (userRepository.isUserNameUsedByOther(userId, newUsername)) {
            throw new IllegalArgumentException("user with newUsername " + newUsername + " already exists");
        }
    }
}
