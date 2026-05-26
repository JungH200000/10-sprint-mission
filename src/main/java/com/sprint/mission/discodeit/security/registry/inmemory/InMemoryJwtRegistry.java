package com.sprint.mission.discodeit.security.registry.inmemory;

import com.nimbusds.jwt.JWTClaimsSet;
import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import com.sprint.mission.discodeit.security.registry.JwtRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// InMemory로 JwtInformation을 저장해 토큰 상태(로그인 상태)를 관리하는 Registry
@Component
@Slf4j
@RequiredArgsConstructor
public class InMemoryJwtRegistry implements JwtRegistry {

    private final JwtTokenProvider jwtTokenProvider;

    // 사용자 ID별 JwtInformation Queue
    private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();

    // 최대 동시 로그인 수
    private final int maxActiveJwtCount = 1;

    /**
     * 로그인 성공 시 JwtInformation 등록(저장)
     */
    @Override
    public JwtInformation registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUserDto().id();

        // 기존 Queue가 없으면 새로운 Queue 생성 후 JwtInformation 저장,
        // 이미 Queue가 있으면 해당 Queue에 JwtInformation 저장
        origin.compute(userId, (id, queue) -> {
            Queue<JwtInformation> jwtInformationQueue = queue != null
                    ? queue
                    : new ConcurrentLinkedDeque<>();

            jwtInformationQueue.add(jwtInformation);

            // 최대 동시 로그인 수 초과 시 가장 오래된 JwtInformation 삭제
            while (jwtInformationQueue.size() > maxActiveJwtCount) {
                jwtInformationQueue.poll();
            }

            return jwtInformationQueue;
        });

        log.debug("[JWT_REGISTER] JWT Information 등록: userId={}", userId);

        return jwtInformation;
    }

    /**
     * UserId로 해당 유저의 모든 JwtInformation 정보 삭제
     */
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        // InMemory(map)에서 제거
        Queue<JwtInformation> jwtInformationQueue = origin.remove(userId);

        // queue가 남아있는지 확인
        if (jwtInformationQueue != null) {
            jwtInformationQueue.clear();
        }

        log.debug("[JWT_INVALIDATE] JWT Information 제거: userId={}", userId);
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> UserId를 가진 사용자의 로그인 상태 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        // userId로 Map에서 JwtInformation 찾기
        Queue<JwtInformation> jwtInformationQueue = origin.get(userId);

        // 해당 userId로 저장된 JwtInformation이 없다면 로그인 상태 아님
        if (jwtInformationQueue == null || jwtInformationQueue.isEmpty()) {
            return false;
        }

        // userId에 매핑된 Queue에서 만료된 Refresh Token 가진 JwtInformation 제거 후
        // 남아있는 JwtInformation 확인
        return removeAndHasActiveJwtInformationByUserId(userId, jwtInformationQueue);
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> filter에서 유효한 Access Token 토큰인지 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }

        try {
            // Access Token에서 userId 추출
            UUID userId = getUserIdInAccessToken(accessToken);

            // origin에서 userId와 매핑된 Queue<JwtInformation> 추출
            Queue<JwtInformation> jwtInformationQueue = origin.get(userId);

            if (jwtInformationQueue == null || jwtInformationQueue.isEmpty()) {
                return false;
            }

            // userId에 매핑된 Queue에서 만료된 Refresh Token 가진 JwtInformation 제거 후
            // active된 JwtInformation 존재여부 확인
            if (!removeAndHasActiveJwtInformationByUserId(userId, jwtInformationQueue)) {
                return false;
            }

            // JwtInformation에서 해당 accessToken이 존재하는지 확인
            return jwtInformationQueue.stream()
                    .anyMatch(jwtInformation ->
                            jwtInformation.getAccessToken().equals(accessToken)
                    );
        } catch (Exception e) {
            // 예외 발생 시 유효하지 않은 Access Token으로 보고 "false" 반환
            return false;
        }
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> 토큰 재발급 시 유효한 Refresh Token인지 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }

        try {
            // Refresh Token에서 userId 추출
            UUID userId = getUserIdInRefreshToken(refreshToken);

            // origin에서 userId와 매핑된 Queue<JwtInformation> 추출
            Queue<JwtInformation> jwtInformationQueue = origin.get(userId);

            if (jwtInformationQueue == null || jwtInformationQueue.isEmpty()) {
                return false;
            }

            // userId에 매핑된 Queue에서 만료된 Refresh Token 가진 JwtInformation 제거 후
            // active된 JwtInformation 존재여부 확인
            if (!removeAndHasActiveJwtInformationByUserId(userId, jwtInformationQueue)) {
                return false;
            }

            // Queue에서 동일한 Refresh Token을 가진 JwtInformation 조회
            Optional<JwtInformation> jwtInformation = jwtInformationQueue.stream()
                    .filter(jwtInfo ->
                            jwtInfo.getRefreshToken().equals(refreshToken)
                    )
                    .findFirst();

            // 해당 JwtInformation의 Refresh Token의 만료 여부 확인
            return jwtInformation
                    .filter(jwtInfo ->
                            hasActiveRefreshToken(jwtInfo)
                    )
                    .isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 재발급 시 토큰 로테이션 수행
     */
    @Override
    public JwtInformation rotateJwtInformation(
            String refreshToken,
            JwtInformation newJwtInformation
    ) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        UUID oldUserId = getUserIdInRefreshToken(refreshToken);
        UUID newUserId = newJwtInformation.getUserDto().id();

        // 기존 userId와 newJwtInformation의 userId 비교
        if (oldUserId.equals(newUserId)) {
            throw new IllegalArgumentException("Refresh Token과 사용자 정보가 일치하지 않습니다.");
        }

        String newAccessToken = newJwtInformation.getAccessToken();
        String newRefreshToken = newJwtInformation.getRefreshToken();

        // 새롭게 Refresh Token와 Access Token 비교
        if ((newRefreshToken == null || newRefreshToken.isEmpty())
                || (newAccessToken == null || newAccessToken.isEmpty())
        ) {
            throw new IllegalArgumentException("새로운 Access/Refresh Token이 비어있습니다.");
        }

        JwtInformation oldJwtInformation = origin.get(oldUserId).stream()
                .filter(jwtInfo ->
                        jwtInfo.getRefreshToken().equals(refreshToken)
                )
                .findFirst()
                .orElseThrow(()->
                        new IllegalArgumentException("기존 JwtInformation을 찾을 수 없음")
                );

        oldJwtInformation.rotate(
                newAccessToken,
                newRefreshToken
        );

        log.debug("[JWT_ROTATE] Jwt Token 로테이션 완료: userId={}", oldUserId);

        return oldJwtInformation;
    }

    /**
     * 만료된 JwtInformation 삭제
     */
    @Override
    public void clearExpiredJwtInformation() {
        origin.forEach((userId, jwtInformationQueue) ->
                removeAndHasActiveJwtInformationByUserId(userId, jwtInformationQueue)
        );

        log.debug("[EXPIRED_JWT_CLEAR] 만료된 Jwt Information 삭제 완료");
    }

    // Access Token 검증 후 userId 추출
    private UUID getUserIdInAccessToken(String accessToken) {
        JWTClaimsSet jwtClaimsSet = jwtTokenProvider.getAndValidateAccessToken(accessToken);
        return UUID.fromString(jwtClaimsSet.getSubject());
    }

    // Refresh Token 검증 후 userId 추출
    private UUID getUserIdInRefreshToken(String refreshToken) {
        JWTClaimsSet jwtClaimsSet = jwtTokenProvider.getAndValidateRefreshToken(refreshToken);
        return UUID.fromString(jwtClaimsSet.getSubject());
    }

    // userId에 매핑된 Queue에서 만료된 Refresh Token 가진 JwtInformation 제거 후
    // active된 JwtInformation 존재여부 확인
    private boolean removeAndHasActiveJwtInformationByUserId(
            UUID userId,
            Queue<JwtInformation> jwtInformationQueue
    ) {
        // Refresh Token 기준으로 로그인 상태 목록 정리
        // Refresh Token이 유효하지 않거나 잘못된 서명이거나 만료 시간이 지난 경우
        jwtInformationQueue.removeIf(jwtInformation ->
                !hasActiveRefreshToken(jwtInformation));

        // 위에서 Refresh Token 삭제 후 남은 JwtInformation이 없다면 Map에서 userId 삭제
        if (jwtInformationQueue.isEmpty()) {
            origin.remove(userId);
            return false;
        }

        // active된 JwtInformation 존재하면
        return true;
    }

    private boolean hasActiveRefreshToken(JwtInformation jwtInformation) {
        return jwtTokenProvider.validateToken(jwtInformation.getRefreshToken());
    }
}
