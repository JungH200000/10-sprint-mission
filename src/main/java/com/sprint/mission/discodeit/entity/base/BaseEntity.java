package com.sprint.mission.discodeit.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Getter
public abstract class BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id; // 객체 식별을 위한 id

    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected Instant createdAt; // 객체 생성 시간: "2026-01-28T00:52:05.985737500Z"
}
