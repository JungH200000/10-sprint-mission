package com.sprint.mission.discodeit.security.registry.inmemory;

import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import com.sprint.mission.discodeit.security.registry.JwtRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// InMemory로 JwtInformation을 저장해 토큰 상태(로그인 상태)를 관리하는 Registry
@Component
@Slf4j
@RequiredArgsConstructor
public class InMemoryJwtRegistry implements JwtRegistry {

    private JwtTokenProvider jwtTokenProvider;

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

        // 기존 JwtInformation이 없으면 새로운 Queue 생성
        origin.compute(userId, (id, jwtInformations) -> {
            Queue<JwtInformation> queue = jwtInformations != null
                    ? jwtInformations
                    : new ConcurrentLinkedDeque<>();

            queue.add(jwtInformation);

            // 최대 동시 로그인 수 초과 시 가장 오래된 JwtInformation 삭제
            while (queue.size() > maxActiveJwtCount) {
                queue.poll();
            }

            return queue;
        });

        log.debug("[JWT_REGISTER] JWT Information 등록: userId={}", userId);

        return jwtInformation;
    }

    /**
     * UserId로 해당 유저의 모든 JwtInformation 정보 삭제
     */
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        origin.remove(userId);

        log.debug("[JWT_INVALIDATE] JWT Information 제거: userId={}", userId);
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> UserId를 가진 사용자의 로그인 상태 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return false;
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> filter에서 유효한 Access Token 토큰인지 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return false;
    }

    /**
     * JwtInformation이 Registry에 존재하는지 확인
     * <br> 토큰 재발급 시 유효한 Refresh Token인지 확인에 사용
     */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return false;
    }

    /**
     * 토큰 재발급 시 토큰 로테이션 수행
     */
    @Override
    public JwtInformation rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        return null;
    }

    /**
     * 만료된 JwtInformation 삭제
     */
    @Override
    public void clearExpiredJwtInformation() {

    }
}
