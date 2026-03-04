package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import lombok.Getter;

@Getter
public class Channel extends BaseUpdatableEntity {
    private ChannelType channelType;
    private String channelName;
    private String channelDescription;

    // 생성자
    public Channel(ChannelType channelType, String channelName, String channelDescription) {
//        this.owner = user; // owner 임명(생성하는 사용자 본인)
        this.channelType = channelType;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id = " + getId() + ", " +
//                "createdAt = " + getCreatedAt() + ", " +
//                "updatedAt = " + getUpdatedAt() + ", " +
                "channel type = " + channelType + ", " +
                "name = " + channelName + ", " + ", " +
//                "description = " + description + ", " +
                "}";
    }

    // update
    public void updateChannelName(String channelName) {
        this.channelName = channelName;
        updateTime();
    }

    public void updateChannelType(ChannelType channelType) {
        this.channelType = channelType;
        updateTime();
    }

    public void updateChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
        updateTime();
    }
}
