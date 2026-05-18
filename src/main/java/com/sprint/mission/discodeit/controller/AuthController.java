package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 권한 관리 Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final UserService userService;
    private final UserStatusService userStatusService;

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ResponseEntity<UserDto> getMe(
            @AuthenticationPrincipal DiscodeitUserDetails principal
    ) {
        UUID userId = principal.getUserDto().id();

        // 사용자 온라인 상태 업데이트
        userStatusService.refreshLastActiveAtByUserId(userId);

        UserDto userDto = userService.find(userId);

        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    /**
     * csrf 토큰 생성
     */
    @RequestMapping(value = "/csrf-token", method = RequestMethod.GET)
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        String token = csrfToken.getToken();

        log.debug("[CSRF_TOKEN_REQUEST] CSRF 토큰 요청");

        return ResponseEntity.status(HttpStatus.NON_AUTHORITATIVE_INFORMATION).build();
    }
}
