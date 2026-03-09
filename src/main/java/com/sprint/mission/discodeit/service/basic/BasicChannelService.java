package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.channel.ChannelDto;
import com.sprint.mission.discodeit.dto.channel.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.*;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicChannelService implements ChannelService {
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final ChannelMapper channelMapper;
    private final UserMapper userMapper;

    @Override
    public ChannelDto createPublicChannel(PublicChannelCreateRequest request) {
        Channel channel = new Channel(
                ChannelType.PUBLIC,
                request.name(),
                request.description()
        );
        channelRepository.save(channel);

        return channelMapper.toDto(channel);
    }

    @Override
    public ChannelDto createPrivateChannel(PrivateChannelCreateRequest request) {
        if (request.participantIds() == null || request.participantIds().isEmpty()) {
            throw new IllegalArgumentException("Private Channel에 참가자 필요함");
        }
        // 인원 검증
        List<User> participants = request.participantIds().stream()
                .map(id -> validateAndGetUserByUserId(id))
                .toList();

        // PRIVATE 채널은 channelName과 channelDescription이 null
        Channel channel = new Channel(ChannelType.PRIVATE, null, null);
        channelRepository.save(channel);

        Instant now = Instant.now();
        participants.forEach(participant -> {
            ReadStatus participantReadStatus = new ReadStatus(participant, channel, now);
            readStatusRepository.save(participantReadStatus);
        });

        return channelMapper.toDto(channel);
    }

    @Transactional(readOnly = true)
    @Override
    public ChannelDto find(UUID channelId) {
        // Channel ID null 검증
        Channel channel = validateAndGetChannelByChannelId(channelId);

        return channelMapper.toDto(channel);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChannelDto> findAllByUserId(UUID userId) {
        // User ID null 검증
        validateUserByUserId(userId);

        // 접근 가능한 전체 채널 조회
        List<Channel> channels = channelRepository.findChannelByUserId(ChannelType.PUBLIC, userId);
        // 접근 가능한 전체 채널 ID
        List<UUID> channelIds = channels.stream()
                .map(channel -> channel.getId())
                .toList();
        // 접근 가능한 Private 채널 ID
        List<UUID> privateChannelIds = channels.stream()
                .filter(channel -> channel.getType().equals(ChannelType.PRIVATE))
                .map(channel -> channel.getId())
                .toList();

        // 각 채널의 마지막 메시지 createdAt 시간
        Map<UUID, Instant> lastMessageAtMap = messageRepository.findLastMessageAtDtoByChannelIds(channelIds).stream()
                .collect(Collectors.toMap(
                                dto -> dto.id(),
                                dto -> dto.lastMessageAt()
                        )
                );

        // 채널별 참가자 목록 조회
        Map<UUID, List<UserDto>> participantMap = readStatusRepository.findAllByChannelIdsWithUserAndChannel(privateChannelIds).stream()
                .collect(Collectors.groupingBy(
                        readStatus -> readStatus.getChannel().getId(),
                        Collectors.mapping(
                                readStatus -> userMapper.toDto(readStatus.getUser()),
                                Collectors.toList()
                        )
                ));

        return channels.stream()
                .map(channel -> channelMapper.toListDto(channel, participantMap, lastMessageAtMap))
                .toList();
    }

    @Override
    public ChannelDto update(UUID channelId, PublicChannelUpdateRequest publicChannelUpdateRequest) {
        // channel 객체 존재 확인
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));

        // PRIVATE Channel일 경우 수정 불가
        if (ChannelType.PRIVATE.equals(channel.getType())) {
            throw new IllegalArgumentException("Private channel cannot be updated");
        }

        // type, name, description이 전부 입력되지 않았거나, 전부 이전과 동일하다면 exception
        validateAllInputDuplicateOrEmpty(publicChannelUpdateRequest, channel);

        Optional.ofNullable(publicChannelUpdateRequest.newName())
                .filter(n -> !channel.getName().equals(n))
                .ifPresent(n -> channel.setName(n));
        Optional.ofNullable(publicChannelUpdateRequest.newDescription())
                .filter(d -> !channel.getDescription().equals(d))
                .ifPresent(d -> channel.setDescription(d));

        channelRepository.save(channel);

        return channelMapper.toDto(channel);
    }

    @Override
    public void delete(UUID channelId) {
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(channelId);

//        messageRepository.deleteAll(messageRepository.findAllByChannelId(channelId));
//        readStatusRepository.deleteAll(readStatusRepository.findAllByChannelIdWithUserAndChannel(channelId));

        channelRepository.deleteById(channelId);
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
        if ((publicChannelUpdateRequest.newName() == null || channel.getName().equals(publicChannelUpdateRequest.newName()))
                && (publicChannelUpdateRequest.newDescription() == null || channel.getDescription().equals(publicChannelUpdateRequest.newDescription()))) {
            throw new IllegalArgumentException("변경사항이 없습니다. 입력 값을 다시 확인하세요.");
        }
    }
}
