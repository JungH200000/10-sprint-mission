package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.channel.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.channel.response.ChannelResponseWithLastMessageAt;
import com.sprint.mission.discodeit.dto.channel.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.entity.Channel;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
    // CRUD(생성, 읽기, 모두 읽기, 수정, 삭제 기능)
    // C. 생성: channelId와 owner 기타 등등 출력
    Channel createPublicChannel(PublicChannelCreateRequest request);
    Channel createPrivateChannel(PrivateChannelCreateRequest request);

    // R. 읽기
    // 특정 채널 정보 읽기
    ChannelResponseWithLastMessageAt findChannelById(UUID channelId);

    // R. 모두 읽기
    // 채널 목록 전체
    List<ChannelResponseWithLastMessageAt> findAllByUserId(UUID userId);

    // U. 수정
    Channel updateChannelInfo(UUID channelId, PublicChannelUpdateRequest publicChannelUpdateRequest);

    // D. 삭제
    void deleteChannel(UUID channelId);
}
