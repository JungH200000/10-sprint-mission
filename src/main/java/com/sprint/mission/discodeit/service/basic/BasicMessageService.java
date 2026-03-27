package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.message.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.message.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.exception.common.NoChangeValueException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public MessageDto create(MessageCreateRequest request, List<MultipartFile> attachments) {
        log.debug("[MESSAGE_CREATE] 메시지 생성 시작: authorId={}, channelId={}, content={}", request.authorId(), request.channelId(), request.content());

        UUID authorId = request.authorId();
        UUID channelId = request.channelId();

        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User author = validateAndGetUserByUserId(authorId);

        // Channel ID null & channel 객체 존재 확인
        Channel channel = validateAndGetChannelByChannelId(channelId);

        Message message = new Message(channel, author, request.content());

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile attachment : attachments) {
                if (attachment == null || attachment.isEmpty()) continue;
                try {
                    byte[] bytes = attachment.getBytes();
                    BinaryContent binaryContent = new BinaryContent(attachment.getOriginalFilename(), attachment.getContentType(), (long) bytes.length);
                    binaryContentRepository.save(binaryContent);
                    binaryContentStorage.put(binaryContent.getId(), bytes);
                    log.info("[BINARY_CONTENT_SAVE] 바이너리 컨텐츠 저장 완료: profileID={}, fileName={}, contentType={}, count={}", binaryContent.getId(), binaryContent.getFileName(), binaryContent.getContentType(), binaryContent.getSize());

                    message.addAttachment(binaryContent);
                } catch (IOException e) {
                    throw new IllegalArgumentException("attachments 업로드 실패", e);
                }
            }
        }
        messageRepository.save(message);
        log.info("[MESSAGE_CREATE] 메시지 생성 완료: messageId={}, authorId={}, channelId={}, content={}, attachmentsCount={}", message.getId(), message.getAuthor().getId(), message.getChannel().getId(), message.getContent(), message.getAttachments().size());

        return messageMapper.toDto(message);
    }

    @Transactional(readOnly = true)
    @Override
    public MessageDto find(UUID messageId) {
        log.debug("[MESSAGE_FIND] 메시지 조회 시작: messageId={}", messageId);

        // Message ID `null` 및 존재 검증
        Message message = validateAndGetMessageByMessageId(messageId);
        log.debug("[MESSAGE_FIND] 메시지 조회 완료: messageId={}, authorId={}, channelId={}, content={}, attachmentsCount={}", message.getId(), message.getAuthor().getId(), message.getChannel().getId(), message.getContent(), message.getAttachments().size());

        return messageMapper.toDto(message);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MessageDto> findAll() {
        log.debug("[MESSAGE_LIST_FIND] 메시지 목록 조회 시작");

        List<MessageDto> messageDtoList = messageRepository.findAll().stream()
                .map(message -> messageMapper.toDto(message))
                .toList();
        log.debug("[MESSAGE_LIST_FIND] 메시지 목록 조회 완료: count={}", messageDtoList.size());

        return messageDtoList;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant cursor, Pageable pageable) {
        log.debug("[MESSAGE_LIST_FIND_BY_CHANNELID] channelId로 메시지 목록 조회 시작: channelId={}, cursor={}, size={}, sort={}", channelId, cursor, pageable.getPageSize(), pageable.getSort());

        // Channel ID null & channel 객체 존재 확인
        validateAndGetChannelByChannelId(channelId);

        Instant createdAt = Optional.ofNullable(cursor)
                .orElse(Instant.now());

        Slice<MessageDto> slice = messageRepository.findAllByChannelId(channelId, createdAt, pageable)
                .map(message -> messageMapper.toDto(message));

        Instant nextCursor = !slice.getContent().isEmpty() ? slice.getContent().get(slice.getContent().size() - 1).createdAt() : null;
        log.debug("[MESSAGE_LIST_FIND_BY_CHANNELID] channelId로 메시지 목록 조회 완료: channelId={}, messageCount={}, nextCursor={}, hasNext={}", channelId, slice.getSize(), nextCursor, slice.hasNext());

        return pageResponseMapper.fromSlice(slice, nextCursor);
    }

    @Override
    public MessageDto update(UUID messageId, MessageUpdateRequest request) {
        log.debug("[MESSAGE_UPDATE] 메시지 수정 시작: messageId={}, newContent={}", messageId, request.newContent());

        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);

        validateAllRequestExistingOrNull(request.newContent());

        message.update(request.newContent());
        log.info("[MESSAGE_UPDATE] 메시지 수정 완료: messageId={}, authorId={}, channelId={}, content={}, attachmentsCount={}", message.getId(), message.getAuthor().getId(), message.getChannel().getId(), message.getContent(), message.getAttachments().size());

        return messageMapper.toDto(message);
    }

    @Override
    public void delete(UUID messageId) {
        log.debug("[MESSAGE_DELETE] 메시지 삭제 시작: messageId={}", messageId);

        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);

        // Channel ID null & channel 객체 존재 확인
        validateAndGetChannelByChannelId(message.getChannel().getId());

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            binaryContentRepository.deleteAll(message.getAttachments());
        }
        messageRepository.deleteById(messageId);
        log.info("[MESSAGE_DELETE] 메시지 삭제 완료: messageId={}", messageId);
    }

    //// validation
    // 로그인 되어있는 user ID null & user 객체 존재 확인
    private User validateAndGetUserByUserId(UUID userId) {
        ValidationMethods.validateId(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("userId", userId));
    }

    // Channel ID null & channel 객체 존재 확인
    private Channel validateAndGetChannelByChannelId(UUID channelId) {
        ValidationMethods.validateId(channelId);
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    }

    // Message ID null & Message 객체 존재 확인
    private Message validateAndGetMessageByMessageId(UUID messageId) {
        ValidationMethods.validateId(messageId);
        return messageRepository.findByIdWithAuthorAndChannel(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message with id " + messageId + " not found"));
    }

    private void validateAllRequestExistingOrNull(String newContent) {
        if (newContent == null) {
            throw new NoChangeValueException("All UpdateRequestField", null);
        }
    }

    // message의 author와 삭제 요청한 user가 동일한지
    private void verifyMessageAuthor(Message message, UUID userId) {
        // message author의 id와 삭제 요청한 user id가 동일한지 확인
        if (!message.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("본인이 작성한 메세지만 수정 가능합니다.");
        }
    }
}
