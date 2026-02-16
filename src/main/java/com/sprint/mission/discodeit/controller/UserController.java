package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.user.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.user.response.UserDto;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusResponse;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 관리 Controller
 */
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Tag(name = "User", description = "User API")
public class UserController {
    private final UserService userService;
    private final UserStatusService userStatusService;

    /**
     * 회원가입(사용자 등록)
     */
    @RequestMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, method = RequestMethod.POST)
    @Operation(summary = "User 등록")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User가 성공적으로 생성됨", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "같은 email이나 username을 사용하는 User가 이미 존재함", content = @Content(examples = @ExampleObject(value = "User with newEmail {newEmail} already exists")))
    })
    public ResponseEntity<?> create(@RequestPart @Valid UserCreateRequest userCreateRequest,
                                    @RequestPart(required = false) @Schema(description = "User 프로필 이미지") MultipartFile profile) {
        User user = userService.createUser(userCreateRequest, profile);
        UserDto result = createUserResponse(user);
        return ResponseEntity.status(201).body(result);
    }

    /**
     * 모든 사용자 조회
     */
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "전체 User 목록 조회")
    @ApiResponse(responseCode = "200", description = "User 목록 조회 성공")
    @Schema(implementation = UserDto.class)
    public ResponseEntity<List<UserDto>> findAll() {
        List<UserDto> result = userService.findAllUsers();

        return ResponseEntity.status(200).body(result);
    }

    /**
     * 사용자 정보 수정
     */
//    @RequestMapping(value = "/me", method = RequestMethod.PATCH)
    @RequestMapping(
            value = "/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = RequestMethod.PATCH
    )
    @Operation(summary = "User 정보 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User 정보가 성공적으로 수정됨", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "같은 newEmail 또는 username을 사용하는 User가 이미 존재함", content = @Content(examples = @ExampleObject(value = "User with newEmail {newEmail} already exists"))),
            @ApiResponse(responseCode = "404", description = "User를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "User with id {userId} not found")))
    })
    public ResponseEntity<UserDto> update(
            @Parameter(description = "수정할 User ID") @PathVariable UUID userId,
            @RequestPart @Valid UserUpdateRequest userUpdateRequest,
            @RequestPart(required = false) @Schema(description = "수정할 User 프로필 이미지") MultipartFile profile) {
        User user = userService.updateUser(userId, userUpdateRequest, profile);
        UserDto result = createUserResponse(user);

        return ResponseEntity.status(200).body(result);
    }

    // 인증 추가 후, 본인이면? /me/online ???

    /**
     * 사용자의 온라인 상태 업데이트
     */
    @RequestMapping(value = "/{userId}/userStatus", method = RequestMethod.PATCH)
    @Operation(summary = "User 온라인 상태 업데이트")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User 온라인 상태가 성공적으로 업데이트됨"),
            @ApiResponse(responseCode = "404", description = "해당 User의 UserStatus를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "UserStatus with userId {userId} not found")))
    })
    public ResponseEntity<UserStatusResponse> updateUserStatusByUserId(
            @Parameter(description = "상태를 변경할 User ID") @PathVariable UUID userId,
            @RequestBody UserStatusUpdateRequest userStatusUpdateRequest) {
        UserStatus userStatus = userStatusService.updateUserStatusByUserId(userId, userStatusUpdateRequest.newLastActiveAt());

        UserStatusResponse result = new UserStatusResponse(
                userStatus.getId(),
                userStatus.getCreatedAt(),
                userStatus.getUpdatedAt(),
                userId,
                userStatus.getLastOnlineTime(),
                userStatus.isOnlineStatus()
        );
        return ResponseEntity.status(200).body(result);
    }

    // 사용자 삭제
    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    @Operation(summary = "User 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User가 성공적으로 삭제됨"),
            @ApiResponse(responseCode = "404", description = "User를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "User with id {id} not found")))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 User ID") @PathVariable UUID userId) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }

    private UserDto createUserResponse(User user) {
        return new UserDto(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getEmail(),
                user.getUsername(),
                user.getBirthday(),
                user.getProfileId(),
                userStatusService.findUserStatusByUserId(user.getId()).isOnlineStatus());
    }
}
