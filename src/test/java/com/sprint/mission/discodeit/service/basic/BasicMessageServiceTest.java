package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.dto.message.MessageDto;
import com.sprint.mission.discodeit.dto.message.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.exception.message.AttachmentsUploadFailedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private BinaryContentStorage binaryContentStorage;

    @Mock
    private PageResponseMapper pageResponseMapper;

    @InjectMocks
    private BasicMessageService basicMessageService;

    UUID authorId;
    User author;
    UserDto authorDto;
    UUID channelId;
    Channel channel;

    @BeforeEach
    void setup() {
        authorId = UUID.randomUUID();
        author = new User("testUser@gmail.com", "testUser", "1234", null);
        ReflectionTestUtils.setField(author, "id", authorId);
        authorDto = new UserDto(author.getId(), author.getUsername(), author.getEmail(), null, true);

        channelId = UUID.randomUUID();
        channel = new Channel(ChannelType.PUBLIC, "channelName", "channelDescription");
        ReflectionTestUtils.setField(channel, "id", channelId);
    }

    @Nested
    @DisplayName("메시지 생성 테스트")
    class createMessage {

        @Test
        @DisplayName("첨부파일이 없는 메시지를 생성할 수 있다.")
        void success_create_message_without_attachments() {
            // given(준비)
            MessageCreateRequest request = new MessageCreateRequest(authorId, channelId, "testMessageContent입니다.");

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

            UUID messageId = UUID.randomUUID();
            Message message = new Message(channel, author, request.content());
            ReflectionTestUtils.setField(message, "id", messageId);
            MessageDto expectedMessageDto = new MessageDto(messageId, message.getCreatedAt(), message.getUpdatedAt(), message.getContent(), message.getChannel().getId(), authorDto, null);

            given(messageMapper.toDto(any(Message.class))).willReturn(expectedMessageDto);

            // when(실행)
            MessageDto result = basicMessageService.create(request, null);

            // then(검증)
            assertEquals(expectedMessageDto, result);
            assertEquals(expectedMessageDto.author(), result.author());
            assertEquals(expectedMessageDto.channelId(), result.channelId());
            assertEquals(expectedMessageDto.content(), result.content());

            verify(userRepository).findById(authorId);
            verify(channelRepository).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository).save(any(Message.class));
            verify(messageMapper).toDto(any(Message.class));
        }

        @Test
        @DisplayName("첨부파일이 있는 메시지를 생성할 수 있다.")
        void success_create_message_with_attachments() throws IOException {
            // given(준비)
            MultipartFile attachment1File = mock(MultipartFile.class);
            byte[] attachment1Bytes = "attachment1".getBytes();
            given(attachment1File.isEmpty()).willReturn(false);
            given(attachment1File.getBytes()).willReturn(attachment1Bytes);
            given(attachment1File.getOriginalFilename()).willReturn("attachment1");
            given(attachment1File.getContentType()).willReturn("image/png");

            MultipartFile attachment2File = mock(MultipartFile.class);
            byte[] attachment2Bytes = "attachment2".getBytes();
            given(attachment2File.isEmpty()).willReturn(false);
            given(attachment2File.getBytes()).willReturn(attachment2Bytes);
            given(attachment2File.getOriginalFilename()).willReturn("attachment2");
            given(attachment2File.getContentType()).willReturn("image/png");

            MessageCreateRequest request = new MessageCreateRequest(authorId, channelId, "testMessageContent입니다.");
            List<MultipartFile> attachments = List.of(attachment1File, attachment2File);

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

            UUID messageId = UUID.randomUUID();
            Message message = new Message(channel, author, request.content());
            ReflectionTestUtils.setField(message, "id", messageId);

            UUID attachment1Id = UUID.randomUUID();
            UUID attachment2Id = UUID.randomUUID();
            BinaryContent attachment1 = new BinaryContent(attachment1File.getOriginalFilename(), attachment1File.getContentType(), attachment1File.getSize());
            BinaryContent attachment2 = new BinaryContent(attachment2File.getOriginalFilename(), attachment2File.getContentType(), attachment2File.getSize());
            ReflectionTestUtils.setField(attachment1, "id", attachment1Id);
            ReflectionTestUtils.setField(attachment2, "id", attachment2Id);

            message.addAttachment(attachment1);
            message.addAttachment(attachment2);

            BinaryContentDto attachment1Dto = new BinaryContentDto(attachment1Id, attachment1.getFileName(), attachment1.getSize(), attachment1.getContentType());
            BinaryContentDto attachment2Dto = new BinaryContentDto(attachment2Id, attachment2.getFileName(), attachment2.getSize(), attachment2.getContentType());

            MessageDto expectedMessageDto = new MessageDto(messageId, message.getCreatedAt(), message.getUpdatedAt(), message.getContent(), message.getChannel().getId(), authorDto, List.of(attachment1Dto, attachment2Dto));

            given(messageMapper.toDto(any(Message.class))).willReturn(expectedMessageDto);

            // when(실행)
            MessageDto result = basicMessageService.create(request, attachments);

            // then(검증)
            assertEquals(expectedMessageDto, result);
            assertEquals(expectedMessageDto.author(), result.author());
            assertEquals(expectedMessageDto.channelId(), result.channelId());
            assertEquals(expectedMessageDto.content(), result.content());

            verify(userRepository).findById(authorId);
            verify(channelRepository).findById(channelId);
            verify(binaryContentRepository, times(2)).save(any(BinaryContent.class));
            verify(binaryContentStorage, times(2)).put(any(), any());
            verify(messageRepository).save(any(Message.class));
            verify(messageMapper).toDto(any(Message.class));
        }

        @Test
        @DisplayName("작성자 ID가 null이면 예외가 발생한다.")
        void fail_create_message_when_authorId_null() {
            // given(준비
            MessageCreateRequest request = new MessageCreateRequest(null, channelId, "testMessageContent입니다.");

            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicMessageService.create(request, null));

            verify(userRepository, never()).findById(authorId);
            verify(channelRepository, never()).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository, never()).save(any(Message.class));
            verify(messageMapper, never()).toDto(any(Message.class));
        }

        @Test
        @DisplayName("특정 ID로 작성자를 찾을 수 없으면 예외가 발생한다.")
        void fail_create_message_when_author_not_found() {
            // given(준비
            MessageCreateRequest request = new MessageCreateRequest(authorId, channelId, "testMessageContent입니다.");

            given(userRepository.findById(authorId)).willReturn(Optional.empty());

            // when(실행), then(검증)
            assertThrows(UserNotFoundException.class,
                    () -> basicMessageService.create(request, null));

            verify(userRepository).findById(authorId);
            verify(channelRepository, never()).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository, never()).save(any(Message.class));
            verify(messageMapper, never()).toDto(any(Message.class));
        }

        @Test
        @DisplayName("채널 ID가 null이면 예외가 발생한다.")
        void fail_create_message_when_channelId_null() {
            // given(준비
            MessageCreateRequest request = new MessageCreateRequest(authorId, null, "testMessageContent입니다.");

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));

            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicMessageService.create(request, null));

            verify(userRepository).findById(authorId);
            verify(channelRepository, never()).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository, never()).save(any(Message.class));
            verify(messageMapper, never()).toDto(any(Message.class));
        }

        @Test
        @DisplayName("특정 ID로 채널을 찾을 수 없으면 예외가 발생한다.")
        void fail_create_message_when_channel_not_found() {
            // given(준비
            MessageCreateRequest request = new MessageCreateRequest(authorId, channelId, "testMessageContent입니다.");

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(channelRepository.findById(channelId)).willReturn(Optional.empty());

            // when(실행), then(검증)
            assertThrows(ChannelNotFoundException.class,
                    () -> basicMessageService.create(request, null));

            verify(userRepository).findById(authorId);
            verify(channelRepository).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository, never()).save(any(Message.class));
            verify(messageMapper, never()).toDto(any(Message.class));
        }

        // AttachmentsUploadFailedException
        @Test
        @DisplayName("첨부파일 업로드를 실패하면 예외가 발생한다.")
        void fail_create_message_when_attachment_upload_fail() throws IOException {
            // given(준비)
            MultipartFile attachment1File = mock(MultipartFile.class);
            byte[] attachment1Bytes = "attachment1".getBytes();
            given(attachment1File.isEmpty()).willReturn(false);
            given(attachment1File.getBytes()).willThrow(new IOException("파일 업로드 실패"));

            MessageCreateRequest request = new MessageCreateRequest(authorId, channelId, "testMessageContent입니다.");
            List<MultipartFile> attachments = List.of(attachment1File);

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

            UUID messageId = UUID.randomUUID();
            Message message = new Message(channel, author, request.content());
            ReflectionTestUtils.setField(message, "id", messageId);

            // when(실행), then(검증)
            assertThrows(AttachmentsUploadFailedException.class,
                    () -> basicMessageService.create(request, attachments));

            verify(userRepository).findById(authorId);
            verify(channelRepository).findById(channelId);

            verify(binaryContentRepository, never()).save(any(BinaryContent.class));
            verify(binaryContentStorage, never()).put(any(), any());

            verify(messageRepository, never()).save(any(Message.class));
            verify(messageMapper, never()).toDto(any(Message.class));
        }
    }

    @Test
    void findAllByChannelId() {
        // given(준비)

        // when(실행)

        // then(검증)
    }

    @Test
    void update() {
        // given(준비)

        // when(실행)

        // then(검증)
    }

    @Test
    void delete() {
        // given(준비)

        // when(실행)

        // then(검증)
    }
}