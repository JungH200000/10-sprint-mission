package com.sprint.mission.discodeit.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected final UUID id; // 객체 식별을 위한 id

    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected final Instant createdAt; // 객체 생성 시간: "2026-01-28T00:52:05.985737500Z"

    protected BaseEntity() {
        // id 초기화
        this.id = UUID.randomUUID();
        // 시간 초기화
        this.createdAt = Instant.now();
    }
}
