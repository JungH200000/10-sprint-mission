package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * 사용자별 마지막으로 확인된 접속 시간을 표현하는 도메인 모델로,
 * 사용자의 온라인 상태를 확인하기 위해 사용
 * <br>
 * 마지막 접속 시간을 기준으로 현재 로그인한 유저를 판단
 * <br>
 * 마지막 접속 시간이 현재 시간 기준 5분 이내라면 접속 중인 유저로 간주
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_statuses")
public class UserStatus extends BaseUpdatableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Instant lastActiveAt;

    // 생성자
    // 유저 생성 시 함께 생성?
    public UserStatus(User user, Instant lastActiveAt) {
        setUser(user);
        this.lastActiveAt = lastActiveAt;
    }

    // 현재 유저 상태 확인(자리 비움, 미접속 등등)
    // 상대도 확인 가능 -> `updateTime()` 미사용이 맞을 것 같음
    public boolean isOnlineStatus() {
        return !Instant.now().isAfter(lastActiveAt.plus(Duration.ofMinutes(5)));
        // !true = 5분 초과
        // !false = 5분 포함 이내
    }

    public void setUser(User user) {
        this.user = user;
        user.setStatus(this);
    }
}
