package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import lombok.Getter;

@Getter
public class Channel extends BaseUpdatableEntity {
    private ChannelType type;
    private String name;
    private String description;

    // 생성자
    public Channel(ChannelType type, String name, String description) {
//        this.owner = user; // owner 임명(생성하는 사용자 본인)
        this.type = type;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id = " + getId() + ", " +
//                "createdAt = " + getCreatedAt() + ", " +
//                "updatedAt = " + getUpdatedAt() + ", " +
                "channel type = " + type + ", " +
                "name = " + name + ", " + ", " +
//                "description = " + description + ", " +
                "}";
    }

    // update
    public void updateChannelName(String channelName) {
        this.name = channelName;
    }

    public void updateChannelType(ChannelType channelType) {
        this.type = channelType;
    }

    public void updateChannelDescription(String channelDescription) {
        this.description = channelDescription;
    }
}
