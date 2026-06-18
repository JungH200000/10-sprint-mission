package com.sprint.mission.discodeit.security.filter.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.sprint.mission.discodeit.exception.security.InvalidJwtTokenException;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import com.sprint.mission.discodeit.security.registry.JwtRegistry;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;

// 요청의 Access Token을 검증하고 Spring Security에 인증 객체를 등록하는 Filter
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    private final DiscodeitUserDetailsService discodeitUserDetailsService;

    // Bearer Access Token이 있으면 인증 시도
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException
    {
        try {
            String accessToken = resolveAccessToken(request);

            // Authorization 헤더에 Bearer 토큰이 없을 경우, accessToken이 `null`
            if (accessToken != null) {
                // 검증된 사용자 정보 + 권한으로 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication = createAuthentication(accessToken);

                // 인증 객체를 SecurityContext에 저장하여 현재 요청을 인증된 상태로 처리
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AuthenticationException e) {
            // 토큰 검증이나 사용자 조회에 실패할 경우
            // 현재 요청의 인증 상태를 제거
            SecurityContextHolder.clearContext();
            throw e;
        }

        // 다음 filter로 이동
        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        // Authorization 헤더에 Bearer 토큰이 없을 경우, AccessToken을 null로 반환
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        // "Bearer "을 제외한 나머지(Access Token)를 가져옴
        String accessToken = authorizationHeader.substring("Bearer ".length());

        if (accessToken.isBlank()) {
            throw new BadCredentialsException("Access Token이 비어있습니다.");
        }

        return accessToken;
    }

    // 검증된 사용자 정보 + 권한으로 인증 객체 생성
    private UsernamePasswordAuthenticationToken createAuthentication(String accessToken) {
        try {
            // JWT 검증, Access Token 여부 확인 후 claims 반환
            // 예외 발생 시, 실패 원인 추가를 위해 "try...catch"문 안에 포함
            JWTClaimsSet jwtClaimsSet = jwtTokenProvider.getAndValidateAccessToken(accessToken);

            // Registry에서 Access Token이 Active인지 확인
            validateActiveAccessToken(accessToken);

            // claims에서 username 조회
            String username = jwtClaimsSet.getStringClaim("username");

            // claims로 사용자 조회
            DiscodeitUserDetails userDetails =
                    (DiscodeitUserDetails) discodeitUserDetailsService.loadUserByUsername(username);

            // 검증된 사용자 정보 + 권한으로 인증 객체 생성
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

        } catch (InvalidJwtTokenException | UsernameNotFoundException | ParseException e) {
            throw new BadCredentialsException("유효하지 않은 Access Token입니다.", e);
        }
    }

    // Access Token이 Active인지 확인
    private void validateActiveAccessToken(String accessToken) {
        if (!jwtRegistry.hasActiveJwtInformationByAccessToken(accessToken)) {
            throw new BadCredentialsException("Active Access Token이 아닙니다.");
        }
    }
}
