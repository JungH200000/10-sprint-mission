package com.sprint.mission.discodeit.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.mission.discodeit.config.jwt.JwtProperties;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.security.userdetails.DiscodeitUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

// JWT 토큰 발급, 재발급, 검증을 담당하는 컴포넌트
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    // claims의 토큰 타입 구분을 위한 문자열
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access_token";
    private static final String REFRESH_TOKEN_TYPE = "refresh_token";

    private final JwtProperties jwtProperties;

    // 인증된 사용자 정보를 담은 Access Token 생성
    public String generateAccessToken(DiscodeitUserDetails discodeitUserDetails) {
        Instant now = Instant.now();
        UserDto userDto = discodeitUserDetails.getUserDto();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userDto.id().toString())
                .claim(TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .claim("email", userDto.email())
                .claim("username", userDto.username())
                .claim("role", userDto.role())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(
                        now.plus(jwtProperties.getAccessTokenExpirationTime())
                ))
                .build();

        return createJwtToken(jwtClaimsSet);
    }

    // 인증된 사용자의 식별자만 담은 Refresh Token 생성
    public String generateRefreshToken(DiscodeitUserDetails discodeitUserDetails) {
        Instant now = Instant.now();
        UserDto userDto = discodeitUserDetails.getUserDto();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userDto.id().toString())
                .claim(TOKEN_TYPE, REFRESH_TOKEN_TYPE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(
                        now.plus(jwtProperties.getRefreshTokenExpirationTime())
                ))
                .build();

        return createJwtToken(jwtClaimsSet);
    }

    // 유효한 Refresh Token과 사용자 정보로 Access Token을 재발급
    public String refreshAccessToken(
            String refreshToken,
            DiscodeitUserDetails discodeitUserDetails
    ) {
        JWTClaimsSet jwtClaimsSet = getAndVerifyJwtToken(refreshToken);

        validateRefreshToken(jwtClaimsSet);

        String userId = discodeitUserDetails.getUserDto().id().toString();

        if (!userId.equals(jwtClaimsSet.getSubject())) {
            throw new IllegalArgumentException("사용자 정보와 일치하지 않음");
        }

        return generateAccessToken(discodeitUserDetails);
    }

    // JWT의 서명과 만료 시간을 검증해 JWT 토큰 유효 여부 확인
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            getAndVerifyJwtToken(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // claims를 HS256 방식으로 서명해 JWT 문자열로 직렬화
    private String createJwtToken(JWTClaimsSet jwtClaimsSet) {
        try {
            JWSSigner jwsSigner = new MACSigner(getJwtSecretKeyBytes());

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    jwtClaimsSet
            );

            signedJWT.sign(jwsSigner);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT 생성에 실패했습니다.", e);
        }
    }

    // JWT 문자열을 파싱한 뒤 서명과 만료 시간을 검증하고 claims를 반환
    private JWTClaimsSet getAndVerifyJwtToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier jwsVerifier = new MACVerifier(getJwtSecretKeyBytes());

            if (!signedJWT.verify(jwsVerifier)) {
                throw new IllegalArgumentException("JWT 서명이 유효하지 않습니다.");
            }

            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            Date expirationTime = jwtClaimsSet.getExpirationTime();

            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new IllegalArgumentException("JWT가 만료되었습니다.");
            }

            return jwtClaimsSet;
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("JWT 검증에 실패");
        }
    }

    // JWT 서명에 사용할 secret key를 UTF-8 byte 배열로 변환
    private byte[] getJwtSecretKeyBytes() {
        return jwtProperties.getJwtSecretKey().getBytes(StandardCharsets.UTF_8);
    }

    // claims의 토큰 타입이 Refresh Token인지 검증
    private void validateRefreshToken(JWTClaimsSet jwtClaimsSet) {
        try {
            String tokenType = jwtClaimsSet.getStringClaim(TOKEN_TYPE);

            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                throw new IllegalArgumentException("Refresh Token이 아닙니다.");
            }

        } catch (ParseException e) {
            throw new IllegalArgumentException("TOKEN_TYPE이 잘못된 형식입니다.");
        }
    }
}
