package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.userstatus.UserStatusDto;
import com.sprint.mission.discodeit.entity.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserStatusMapper {

    // source : entity의 필드
    // target : dto의 필드
    @Mapping(source = "user.id", target = "userId")
    UserStatusDto toDto(UserStatus userStatus);
}
