package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.auth.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final UserService userService;
    private final UserStatusService userStatusService;
    private final AuthService authService;

    // csrf 토큰 생성 API
    @RequestMapping(value = "/csrf-token", method = RequestMethod.GET)
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        String token = csrfToken.getToken();

        log.debug("[CSRF_TOKEN_REQUEST] CSRF 토큰 요청: token={}", token);

        return ResponseEntity.status(HttpStatus.NON_AUTHORITATIVE_INFORMATION).build();
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @Operation(summary = "세션을 활용한 현재 사용자 정보 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User 온라인 상태가 성공적으로 업데이트됨"),
            @ApiResponse(responseCode = "404", description = "User나 UserStatus를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "User/UserStatus with id {id} not found")))
    })
    public ResponseEntity<UserDto> getMe(
            @AuthenticationPrincipal DiscodeitUserDetails principal
    ) {
        UUID userId = principal.getUserDto().id();

        // 사용자 온라인 상태 업데이트
        userStatusService.refreshLastActiveAtByUserId(userId);

        UserDto userDto = userService.find(userId);

        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @RequestMapping(value = "/role", method = RequestMethod.PUT)
    @Operation(summary = "사용자 권한 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User 권한이 성공적으로 업데이트됨"),
            @ApiResponse(responseCode = "404", description = "User를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "User with id {id} not found")))
    })
    public ResponseEntity<UserDto> updateUserRole(@RequestBody UserRoleUpdateRequest request) {
        UserDto userDto = authService.updateUserRole(request);

        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }
}
