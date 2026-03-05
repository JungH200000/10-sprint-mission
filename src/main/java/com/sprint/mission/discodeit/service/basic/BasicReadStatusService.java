package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.readstatus.ReadStatusDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicReadStatusService implements ReadStatusService {
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;

    @Override
    public ReadStatusDto create(ReadStatusCreateRequest readStatusCreateRequest) {
        UUID userId = readStatusCreateRequest.userId();
        UUID channelId = readStatusCreateRequest.channelId();
        Instant lastReadAt = readStatusCreateRequest.lastReadAt();
        // user 객체 존재 확인
        User user = validateAndGetUserByUserId(userId);
        // channel 객체 존재 확인
        Channel channel = validateAndGetChannelByChannelId(channelId);

        if (readStatusRepository.existsReadStatusByUserIdAndChannelId(userId, channelId)) {
            throw new IllegalArgumentException("ReadStatus with id " + userId + " and channelId " + channelId + " already exists.");
        }

        ReadStatus readStatus = new ReadStatus(user, channel, lastReadAt);

        readStatusRepository.save(readStatus);
        return createReadStatusDto(readStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public ReadStatusDto find(UUID readStatusId) {
        ReadStatus readStatus = validateAndGetReadStatusByReadStatusId(readStatusId);
        return createReadStatusDto(readStatus);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReadStatusDto> findAllByUserId(UUID userId) {
        // user ID null & user 객체 존재 확인
        validateUserByUserId(userId);

        return readStatusRepository.findAllByUserIdWithUserAndChannel(userId).stream()
                .map(readStatus -> createReadStatusDto(readStatus))
                .toList();
    }

    @Override
    public ReadStatusDto update(UUID readStatusId, ReadStatusUpdateRequest readStatusUpdateRequest) {
        ReadStatus readStatus = validateAndGetReadStatusByReadStatusId(readStatusId);

        readStatus.updateLastReadTime(readStatusUpdateRequest.newLastReadAt());
        readStatusRepository.save(readStatus);

        return createReadStatusDto(readStatus);
    }

    @Override
    public void delete(UUID readStatusId) {
        validateReadStatusByReadStatusId(readStatusId);
        readStatusRepository.deleteById(readStatusId);
    }

    private ReadStatusDto createReadStatusDto(ReadStatus readStatus) {
        return new ReadStatusDto(
                readStatus.getId(),
                readStatus.getCreatedAt(),
                readStatus.getUpdatedAt(),
                readStatus.getUser().getId(),
                readStatus.getChannel().getId(),
                readStatus.getLastReadAt()
        );
    }

    //// validation
    // user ID null & user 객체 존재 확인
    public User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public void validateUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public Channel validateAndGetChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }
    public void validateChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }
    public ReadStatus validateAndGetReadStatusByReadStatusId(UUID readStatusId) {
        ValidationMethods.validateId(readStatusId);
        return readStatusRepository.findByIdWithUserAndChannel(readStatusId)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus with id " + readStatusId + " not found"));
    }
    public void validateReadStatusByReadStatusId(UUID readStatusId) {
        ValidationMethods.validateId(readStatusId);
        readStatusRepository.findById(readStatusId)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus with id " + readStatusId + " not found"));
    }
}
