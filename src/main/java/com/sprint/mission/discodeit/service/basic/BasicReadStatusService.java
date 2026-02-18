package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.readstatus.response.ReadStatusResponse;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasicReadStatusService implements ReadStatusService {
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;

    @Override
    public ReadStatus createReadStatus(ReadStatusCreateRequest readStatusCreateRequest) {
        UUID userId = readStatusCreateRequest.userId();
        UUID channelId = readStatusCreateRequest.channelId();
        Instant lastReadAt = readStatusCreateRequest.lastReadAt();
        // user 객체 존재 확인
        validateUserByUserId(userId);
        // channel 객체 존재 확인
        validateChannelByChannelId(channelId);

        if (readStatusRepository.existReadStatus(userId, channelId)) {
            throw new IllegalArgumentException("ReadStatus with userId " + userId + " and channelId " + channelId + " already exists.");
        }

        ReadStatus readStatus = new ReadStatus(userId, channelId, lastReadAt);

        readStatusRepository.save(readStatus);
        return readStatus;
    }

    @Override
    public ReadStatus findReadStatusById(UUID readStatusId) {
        return validateAndGetReadStatusByReadStatusId(readStatusId);
    }

    @Override
    public List<ReadStatusResponse> findAllByUserId(UUID userId) {
        // user ID null & user 객체 존재 확인
        validateUserByUserId(userId);
        List<ReadStatus> readStatuses = readStatusRepository.findAllByUserId(userId);
        List<ReadStatusResponse> readStatusInfos = new ArrayList<>();
        for (ReadStatus readStatus : readStatuses) {
            readStatusInfos.add(new ReadStatusResponse(
                    readStatus.getId(),
                    readStatus.getCreatedAt(),
                    readStatus.getUpdatedAt(),
                    readStatus.getUserId(),
                    readStatus.getChannelId(),
                    readStatus.getLastReadAt()
            ));
        }
        return readStatusInfos;
    }

    @Override
    public ReadStatus updateReadStatus(UUID readStatusId, ReadStatusUpdateRequest readStatusUpdateRequest) {
        ReadStatus readStatus = validateAndGetReadStatusByReadStatusId(readStatusId);

        readStatus.updateLastReadTime(readStatusUpdateRequest.newLastReadAt());
        readStatusRepository.save(readStatus);

        return readStatus;
    }

    @Override
    public void deleteReadStatus(UUID readStatusId) {
        validateReadStatusByReadStatusId(readStatusId);
        readStatusRepository.delete(readStatusId);
    }

    //// validation
    // user ID null & user 객체 존재 확인
    public void validateUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
    }
    public void validateChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }
    public ReadStatus validateAndGetReadStatusByReadStatusId(UUID readStatusId) {
        ValidationMethods.validateId(readStatusId);
        return readStatusRepository.findById(readStatusId)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus with id " + readStatusId + " not found"));
    }
    public void validateReadStatusByReadStatusId(UUID readStatusId) {
        ValidationMethods.validateId(readStatusId);
        readStatusRepository.findById(readStatusId)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus with id " + readStatusId + " not found"));
    }
}
