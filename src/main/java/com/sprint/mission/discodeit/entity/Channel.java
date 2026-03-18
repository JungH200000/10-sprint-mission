package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "channels")
public class Channel extends BaseUpdatableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Column
    private String name;

    @Column
    private String description;

    // 생성자
    public Channel(ChannelType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }
}
