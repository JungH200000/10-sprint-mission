package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.message.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.validation.ValidationMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public Message createMessage(MessageCreateRequest messageCreateRequest, List<MultipartFile> attachments) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        User author = userRepository.findById(messageCreateRequest.authorId())
                .orElseThrow(() -> new NoSuchElementException("Author with id " + messageCreateRequest.authorId() + " not found"));

        // Channel ID null & channel 객체 존재 확인
        Channel channel = channelRepository.findById(messageCreateRequest.channelId())
                .orElseThrow(() -> new NoSuchElementException("Channel with id " + messageCreateRequest.channelId() + " not found"));

        Message message = new Message(channel, author, messageCreateRequest.content());

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile attachment : attachments) {
                if (attachment == null || attachment.isEmpty()) continue;
                try {
                    byte[] bytes = attachment.getBytes();
                    BinaryContent binaryContent = new BinaryContent(
                            attachment.getOriginalFilename(),
                            attachment.getContentType(),
                            bytes,
                            (long) bytes.length
                    );
                    message.addAttachmentId(binaryContent.getId());
                    binaryContentRepository.save(binaryContent);
                } catch (IOException e) {
                    throw new IllegalArgumentException("attachments 업로드 실패", e);
                }
            }
        }
        messageRepository.save(message);
        return message;
    }

    @Override
    public Message findMessageById(UUID messageId) {
        // Message ID `null` 검증
        ValidationMethods.validateId(messageId);

        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("해당 메세지가 없습니다."));
    }

    @Override
    public List<Message> findAllMessages() {
        return messageRepository.findAll();
    }

    @Override
    public List<Message> findAllByChannelId(UUID channelId) {
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(channelId);

        return messageRepository.findByChannelId(channelId);
    }

    @Override
    public List<Message> findUserMessagesByUserId(UUID userId) {
        // 로그인 되어있는 user ID null / user 객체 존재 확인
        validateUserByUserId(userId);

        return messageRepository.findByAuthorId(userId);
    }

    @Override
    public Message updateMessageContent(UUID messageId, MessageUpdateRequest messageUpdateRequest) {
        // Message ID null & Message 객체 존재 확인
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message with id " + messageId + " not found"));

        message.updateContent(messageUpdateRequest.newContent());
        messageRepository.save(message);
        return message;
    }

    @Override
    public void deleteMessageByUserId(UUID userId, UUID messageId) {
        // 요청자의 user ID null / user 객체 존재 확인
        validateUserByUserId(userId);
        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);
        // User ID null / user 객체 존재 확인
        validateUserByUserId(userId);
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(message.getChannel().getId());

        if (message.getAttachmentIds() != null && !message.getAttachmentIds().isEmpty()) {
            for (UUID attachmentId : message.getAttachmentIds()) {
                binaryContentRepository.delete(attachmentId);
            }
        }
        messageRepository.delete(messageId);
    }

    @Override
    public void deleteMessage(UUID messageId) {
        // Message ID null & Message 객체 존재 확인
        Message message = validateAndGetMessageByMessageId(messageId);
        // User ID null / user 객체 존재 확인
        validateUserByUserId(message.getAuthor().getId());
        // Channel ID null & channel 객체 존재 확인
        validateChannelByChannelId(message.getChannel().getId());

        if (message.getAttachmentIds() != null && !message.getAttachmentIds().isEmpty()) {
            for (UUID attachmentId : message.getAttachmentIds()) {
                binaryContentRepository.delete(attachmentId);
            }
        }
        messageRepository.delete(messageId);
    }

    //// validation
    // 로그인 되어있는 user ID null & user 객체 존재 확인
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
        return messageRepository.findById(messageId)
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
