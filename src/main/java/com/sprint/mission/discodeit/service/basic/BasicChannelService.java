package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.channel.response.ChannelDtoWithLastMessageAt;
import com.sprint.mission.discodeit.dto.channel.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.repository.*;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Validated
public class BasicChannelService implements ChannelService {
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public Channel createPublicChannel(PublicChannelCreateRequest request) {
        Channel channel = new Channel(
                ChannelType.PUBLIC,
                request.channelName(),
                request.channelDescription()
        );
        channelRepository.save(channel);

        return channel;
    }

    @Override
    public Channel createPrivateChannel(PrivateChannelCreateRequest request) {
        // PRIVATE 채널은 channelName과 channelDescription이 null
        Channel channel = new Channel(
                ChannelType.PRIVATE,
                null,
                null
        );

        if (request.participantIds() != null && !request.participantIds().isEmpty()) {
            for (UUID participantId : request.participantIds()) {
                validateUserByUserId(participantId);
                ReadStatus participantReadStatus = new ReadStatus(participantId, channel.getId(), Instant.now());
                readStatusRepository.save(participantReadStatus);
            }
        }
        channelRepository.save(channel);

        return channel;
    }

    @Override
    public ChannelDtoWithLastMessageAt findChannelById(UUID channelId) {
        // Channel ID null 검증
        ValidationMethods.validateId(channelId);
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("해당 채널이 없습니다."));

        Instant lastMessageTime = messageRepository.findByChannelId(channelId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .map(message -> message.getCreatedAt())
                .orElse(null);

        return createChannelPublicResponse(channel, lastMessageTime);
    }

    @Override
    public List<ChannelDtoWithLastMessageAt> findAllByUserId(UUID userId) {
        // User ID null 검증
        validateUserByUserId(userId);

        // 유저가 참여한 모든 채널
        List<UUID> participatedChannelList = readStatusRepository.findAllByUserId(userId).stream()
                .map(readStatus -> readStatus.getChannelId())
                .toList();

        // 모든 채널에서 PUBLIC인 채널 전체와 유저가 참여한 모든 채널 비교?
        return channelRepository.findAll().stream()
                .filter(channel -> channel.getChannelType() == ChannelType.PUBLIC || participatedChannelList.contains(channel.getId()))
                .map(channel -> findChannelById(channel.getId()))
                .toList();
    }

    @Override
    public Channel updateChannelInfo(UUID channelId, PublicChannelUpdateRequest publicChannelUpdateRequest) {
        // channel 객체 존재 확인
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));

        // PRIVATE Channel일 경우 수정 불가
        if (ChannelType.PRIVATE.equals(channel.getChannelType())) {
            throw new IllegalArgumentException("Private channel cannot be updated");
        }

        // type, name, description이 전부 입력되지 않았거나, 전부 이전과 동일하다면 exception
        validateAllInputDuplicateOrEmpty(publicChannelUpdateRequest, channel);

        Optional.ofNullable(publicChannelUpdateRequest.newName())
                .filter(n -> !channel.getChannelName().equals(n))
                .ifPresent(n -> channel.updateChannelName(n));
        Optional.ofNullable(publicChannelUpdateRequest.newDescription())
                .filter(d -> !channel.getChannelDescription().equals(d))
                .ifPresent(d -> channel.updateChannelDescription(d));

        channelRepository.save(channel);
        return channel;
    }

    @Override
    public void deleteChannel(UUID channelId) {
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(channelId);

        for (Message message : messageRepository.findByChannelId(channelId)) {
            validateUserByUserId(message.getAuthor().getId());

            if (message.getAttachmentIds() != null && !message.getAttachmentIds().isEmpty()) {
                for (UUID attachmentId : message.getAttachmentIds()) {
                    binaryContentRepository.delete(attachmentId);
                }
            }
            messageRepository.delete(message.getId());
        }
        for (ReadStatus readStatus : readStatusRepository.findAllByChannelId(channelId)) {
            readStatusRepository.delete(readStatus.getId());
        }

        channelRepository.delete(channelId);
    }

    private ChannelDtoWithLastMessageAt createChannelPublicResponse(Channel channel, Instant lastMessageTime) {
        List<UUID> participantIds = new ArrayList<>();
        if (channel.getChannelType().equals(ChannelType.PRIVATE)) {
            readStatusRepository.findAllByChannelId(channel.getId()).stream()
                    .map(readStatus -> readStatus.getUserId())
                    .forEach(participantId -> participantIds.add(participantId));
        }
        return new ChannelDtoWithLastMessageAt(
                channel.getId(),
                channel.getChannelType(),
                channel.getChannelName(),
                channel.getChannelDescription(),
                participantIds,
                lastMessageTime
        );
    }

    // validation
    //로그인 되어있는 user ID null & user 객체 존재 확인
    public void validateUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public void validateChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }
    public Channel validateAndGetChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }
    // type, name, channelDescription이 전부 입력되지 않았거나, 전부 이전과 동일하다면 exception
    private void validateAllInputDuplicateOrEmpty(PublicChannelUpdateRequest publicChannelUpdateRequest, Channel channel) {
        if ((publicChannelUpdateRequest.newName() == null || channel.getChannelName().equals(publicChannelUpdateRequest.newName()))
                && (publicChannelUpdateRequest.newDescription() == null || channel.getChannelDescription().equals(publicChannelUpdateRequest.newDescription()))) {
            throw new IllegalArgumentException("변경사항이 없습니다. 입력 값을 다시 확인하세요.");
        }
    }
    // channel owner의 user ID와 owner의 user ID가 동일한지 확인
    public void verifyChannelOwner(Channel channel, UUID ownerId) {
        // channel owner의 user Id와 owner의 user Id 동일한지 확인
        if (!channel.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("채널 owner만 수행 가능합니다.");
        }
    }

    @Override
    public Channel joinChannel(UUID userId, UUID channelId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);
        // Channel ID null & channel 객체 존재 확인
        ValidationMethods.validateId(channelId);
        Channel channel = validateAndGetChannelByChannelId(channelId);

        // 이미 참여한 채널인지 검증
        if (channel.getChannelMembersList().stream()
                .anyMatch(u -> u.getId().equals(userId))) {
            throw new IllegalStateException("이미 참여한 채널입니다.");
        }

//        linkMemberAndChannel(user, channel);
        ReadStatus readStatus = new ReadStatus(userId, channelId, Instant.now());

        channelRepository.save(channel);
        // `linkMemberAndChannel` 메소드로 user의 joinChannelList에 해당 channel 추가 후, 저장
        userRepository.save(user);
        readStatusRepository.save(readStatus);

        return channel;
    }

    @Override
    public Channel leaveChannel(UUID userId, UUID channelId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);
        // Channel ID null & channel 객체 존재 확인
        ValidationMethods.validateId(channelId);
        Channel channel = validateAndGetChannelByChannelId(channelId);

        // 참여한 채널인지 확인
        if (!channel.getChannelMembersList().stream()
                .anyMatch(u -> u.getId().equals(userId))) {
            throw new IllegalStateException("참여하지 않은 채널입니다. 참여하지 않았으므로 나갈 수도 없습니다.");
        }

        ReadStatus readStatus = readStatusRepository.findByUserIdAndChannelId(userId, channelId)
                .orElseThrow(() -> new NoSuchElementException("userId와 channelId에 해당하는 readStatus가 없습니다."));

        channelRepository.save(channel);
        // `unlinkMemberAndChannel` 메소드로 user의 joinChannelList에 해당 channel 삭제 후, 저장
        userRepository.save(user);
        readStatusRepository.delete(readStatus.getId());

        return channel;
    }
}
