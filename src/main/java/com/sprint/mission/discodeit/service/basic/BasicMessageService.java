package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.message.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.message.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicMessageService implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final MessageMapper messageMapper;
    private final BinaryContentStorage binaryContentStorage;
    private final PageResponseMapper pageResponseMapper;

    @Override
    public MessageDto create(MessageCreateRequest messageCreateRequest, List<MultipartFile> attachments) {
        UUID authorId = messageCreateRequest.authorId();
        UUID channelId = messageCreateRequest.channelId();

        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User author = validateAndGetUserByUserId(authorId);

        // Channel ID null & channel 객체 존재 확인
        Channel channel = validateAndGetChannelByChannelId(channelId);

        Message message = new Message(channel, author, messageCreateRequest.content());

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile attachment : attachments) {
                if (attachment == null || attachment.isEmpty()) continue;
                try {
                    byte[] bytes = attachment.getBytes();
                    BinaryContent binaryContent = new BinaryContent(attachment.getOriginalFilename(), attachment.getContentType(), (long) bytes.length);
                    binaryContentRepository.save(binaryContent);
                    binaryContentStorage.put(binaryContent.getId(), bytes);
                    message.addAttachment(binaryContent);
                } catch (IOException e) {
                    throw new IllegalArgumentException("attachments 업로드 실패", e);
                }
            }
        }
        messageRepository.save(message);

        return messageMapper.toDto(message);
    }

    @Transactional(readOnly = true)
    @Override
    public MessageDto find(UUID messageId) {
        // Message ID `null` 및 존재 검증
        Message message = validateAndGetMessageByMessageId(messageId);

        return messageMapper.toDto(message);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MessageDto> findAll() {
        return messageRepository.findAll().stream()
                .map(message -> messageMapper.toDto(message))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant cursor, Pageable pageable) {
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(channelId);
        Instant createdAt = Optional.ofNullable(cursor)
                .orElse(Instant.now());

        Slice<MessageDto> slice = messageRepository.findAllByChannelId(channelId, createdAt, pageable)
                .map(message -> messageMapper.toDto(message));

        Instant nextCursor = !slice.getContent().isEmpty() ? slice.getContent().get(slice.getContent().size() - 1).createdAt() : null;

        return pageResponseMapper.fromSlice(slice, nextCursor);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MessageDto> findUserMessagesByUserId(UUID userId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        validateUserByUserId(userId);

        return messageRepository.findAllByAuthorId(userId).stream()
                .map(message -> messageMapper.toDto(message))
                .toList();
    }

    @Override
    public MessageDto update(UUID messageId, MessageUpdateRequest messageUpdateRequest) {
        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);

        message.setContent(messageUpdateRequest.newContent());
        messageRepository.save(message);
        return messageMapper.toDto(message);
    }

    @Override
    public void deletByIdAndUserId(UUID userId, UUID messageId) {
        // 요청자의 user ID null / user 객체 존재 확인
        validateUserByUserId(userId);
        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(message.getChannel().getId());

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            binaryContentRepository.deleteAll(message.getAttachments());
        }
        messageRepository.deleteById(messageId);
    }

    @Override
    public void delete(UUID messageId) {
        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);
//        // User ID null / user 객체 존재 확인
//        validateUserByUserId(message.getAuthor().getId());
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(message.getChannel().getId());

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            binaryContentRepository.deleteAll(message.getAttachments());
        }
        messageRepository.deleteById(messageId);
    }

    //// validation
    // 로그인 되어있는 user ID null & user 객체 존재 확인
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

    // Channel ID null & channel 객체 존재 확인
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

    // Message ID null & Message 객체 존재 확인
    public Message validateAndGetMessageByMessageId(UUID messageId) {
        ValidationMethods.validateId(messageId);
        return messageRepository.findByIdWithAuthorAndChannel(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message with id " + messageId + " not found"));
    }

    // message의 author와 삭제 요청한 user가 동일한지
    public void verifyMessageAuthor(Message message, UUID userId) {
        // message author의 id와 삭제 요청한 user id가 동일한지 확인
        if (!message.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("본인이 작성한 메세지만 수정 가능합니다.");
        }
    }
}
