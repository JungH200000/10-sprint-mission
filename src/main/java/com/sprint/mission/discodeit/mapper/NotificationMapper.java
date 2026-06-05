package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.notification.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public abstract class NotificationMapper {

    public abstract NotificationDto toDto(Notification notification);
}
