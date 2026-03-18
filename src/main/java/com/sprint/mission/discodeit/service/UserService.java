package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.user.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    // CRUD(생성, 읽기, 모두 읽기, 수정, 삭제 기능)
    // C. 생성: userId와 기타 등등 출력
    UserDto create(UserCreateRequest request, MultipartFile profile);

    // R. 읽기
    UserDto find(UUID userId);

    // R. 모두 읽기
    // 모든 사용자
    List<UserDto> findAll();

    // U. 수정
    UserDto update(UUID userId, UserUpdateRequest request, MultipartFile profile);

    // D. 삭제
    void delete(UUID userId);
}
