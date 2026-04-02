package com.sprint.mission.discodeit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.dto.channel.ChannelDto;
import com.sprint.mission.discodeit.dto.message.MessageDto;
import com.sprint.mission.discodeit.dto.message.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.GlobalExceptionHandler;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.message.MessageNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebMvcTest(controllers = MessageController.class)
@Import(GlobalExceptionHandler.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청 시뮬레이터

    @Autowired
    private ObjectMapper om; // JSON 직렬화/역직렬화 도구

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private UserService userService;

    // `@EnableJpaAuditing`이 애플리케이션 시작 클래스에 설정되어 있어서 JPA Auditing가 활성화됨
    // `jpaAuditingHandler`는 `jpaMappingContext`를 필요로 함
    // 근데 API 슬라이스 테스트에는 Entity 메타 모델이 없어서 오류가 남
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;


    Instant now;
    Instant nowMinus5;
    Instant nowMinus10;
    Instant nowMinus15;
    Instant nowMinus20;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        nowMinus5 = now.minus(5, ChronoUnit.MINUTES);
        nowMinus10 = now.minus(10, ChronoUnit.MINUTES);
        nowMinus15 = now.minus(15, ChronoUnit.MINUTES);
        nowMinus20 = now.minus(20, ChronoUnit.MINUTES);
    }

    private UserDto createUserDto(String email, String username, BinaryContentDto profile, boolean isOnline) {
        UUID authorId = UUID.randomUUID();
        return new UserDto(authorId, username, email, profile, isOnline);
    }

    private ChannelDto createChannelDto(ChannelType type, String name, String description,  List<UserDto> participants, Instant lastMessageAt) {
        UUID channelId = UUID.randomUUID();
        return new ChannelDto(channelId, type, name, description, participants, lastMessageAt);
    }

    private MessageDto createMessageDto(String content, Instant createdAt, Instant updatedAt, UUID channelId, UserDto author, List<BinaryContentDto> attachments) {
        UUID messageId = UUID.randomUUID();
        return new MessageDto(messageId, createdAt, updatedAt, content, channelId, author, attachments);
    }

    private BinaryContentDto createBinaryContent(String fileName, String contentType, Long size) {
        UUID binaryContentId = UUID.randomUUID();
        return new BinaryContentDto(binaryContentId, fileName, size, contentType);
    }

    @Nested
    @DisplayName("메시지 생성 API 테스트")
    class createMessage {

        @Test
        @DisplayName("메시지를 생성하면 201 상태 코드와 메시지 정보를 반환한다.")
        void success_create_message() throws Exception {
            // given(준비)
            BinaryContentDto profileDto = createBinaryContent("profileFile", "ProfileFileContentType", 1L);

            BinaryContentDto attachmentDto1 = createBinaryContent("file1", "contentType1", 1L);
            BinaryContentDto attachmentDto2 = createBinaryContent("file2", "contentType2", 2L);
            List<BinaryContentDto> attachments = List.of(attachmentDto1, attachmentDto2);

            UserDto authorDto = createUserDto("test@gmail.com", "test", profileDto, true);
            ChannelDto channelDto = createChannelDto(ChannelType.PUBLIC, "testChannel", "testChannel입니다.", null, Instant.now());

            MessageCreateRequest request = new MessageCreateRequest(authorDto.id(), channelDto.id(), "testMessageContent");

            Instant createdAt = now;
            Instant updatedAt = now;
            MessageDto expectedMessageDto = createMessageDto(request.content(), createdAt, updatedAt, channelDto.id(), authorDto, attachments);
            
            // `mockMvc.perform(...)`에서 multipart 요청을 만들고,
            // 이 요청이 controller의 `create` 메서드 파라미터로 바인딩 하기 위해 만듦
            MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(request));
            MockMultipartFile attachment1 = new MockMultipartFile("attachments", "image1.png", MediaType.IMAGE_PNG_VALUE, "image1".getBytes());
            MockMultipartFile attachment2 = new MockMultipartFile("attachments", "image2.png", MediaType.IMAGE_PNG_VALUE, "image2".getBytes());

            given(messageService.create(eq(request), anyList())).willReturn(expectedMessageDto);

            // when(실행), then(검증)
            mockMvc.perform(multipart("/api/messages")
                            .file(requestPart)
                            .file(attachment1)
                            .file(attachment2)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(expectedMessageDto.id().toString()))
                    .andExpect(jsonPath("$.channelId").value(expectedMessageDto.channelId().toString()))
                    .andExpect(jsonPath("$.author.id").value(expectedMessageDto.author().id().toString()))
                    .andExpect(jsonPath("$.author.profile.id").value(expectedMessageDto.author().profile().id().toString()))
                    .andExpect(jsonPath("$.attachments", hasSize(2)));

            verify(messageService).create(any(MessageCreateRequest.class), anyList());
        }

        @Test
        @DisplayName("채널을 찾을 수 없으면 404 상태 코드와 ChannelNotFoundException 예외 응답이 발생한다.")
        void fail_create_message_when_channel_not_found() throws Exception {
            // given(준비)
            UserDto userDto = createUserDto("test", "test", null, true);
            UUID channelId = UUID.randomUUID();

            MessageCreateRequest request = new MessageCreateRequest(userDto.id(), channelId, "testMessageContent");

            MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(request));

            given(messageService.create(any(MessageCreateRequest.class), isNull())).willThrow(new ChannelNotFoundException(channelId));

            // when(실행), then(검증)
            mockMvc.perform(multipart("/api/messages")
                            .file(requestPart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CHANNEL_NOT_FOUND.toString()))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.exceptionType").value(ChannelNotFoundException.class.getSimpleName()))
                    .andExpect(jsonPath("$.details.channelId").value(channelId.toString()));
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 404 상태 코드와 UserNotFoundException 예외 응답이 발생한다.")
        void fail_create_message_when_user_not_found() throws Exception {
            // given(준비)
            UUID userId = UUID.randomUUID();
            ChannelDto channelDto = createChannelDto(ChannelType.PUBLIC, "testChannel", "testChannel입니다.", null, Instant.now());

            MessageCreateRequest request = new MessageCreateRequest(userId, channelDto.id(), "testMessageContent");

            MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(request));

            given(messageService.create(any(MessageCreateRequest.class), isNull())).willThrow(new UserNotFoundException("userId", userId));

            // when(실행), then(검증)
            mockMvc.perform(multipart("/api/messages")
                            .file(requestPart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.toString()))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.exceptionType").value(UserNotFoundException.class.getSimpleName()))
                    .andExpect(jsonPath("$.details.userId").value(userId.toString()));
        }
    }

    @Nested
    @DisplayName("메시지 목록 API 테스트")
    class findMessageListByChannelId {
        @Test
        @DisplayName("특정 채널의 메시지 목록을 조회하면 200 상태 코드와 메시지 목록이 반환된다.")
        void success_find_message_list_by_channelId() throws Exception {
            // given(준비)
            UUID requestChannelId = UUID.randomUUID();
            Instant cursor = now;
            Pageable pageable = PageRequest.of(1, 5, Sort.by("createdAt").descending());

            BinaryContentDto attachmentDto1 = createBinaryContent("file1", "contentType1", 1L);
            BinaryContentDto attachmentDto2 = createBinaryContent("file2", "contentType2", 2L);
            List<BinaryContentDto> attachments = List.of(attachmentDto1, attachmentDto2);

            UserDto authorDto1 = createUserDto("test1@gmail.com", "test1", null, true);
            UserDto authorDto2 = createUserDto("test2@gmail.com", "test2", null, true);

            List<MessageDto> messageDtoList = List.of(
                    createMessageDto("test1MessageContent", nowMinus10, nowMinus20, requestChannelId, authorDto1, null),
                    createMessageDto("test2MessageContent", nowMinus10, nowMinus15, requestChannelId, authorDto1, null),
                    createMessageDto("test3MessageContent", nowMinus5, nowMinus10, requestChannelId, authorDto1, attachments),
                    createMessageDto("test4MessageContent", nowMinus5, nowMinus10, requestChannelId, authorDto2, null),
                    createMessageDto("test5MessageContent", now, now, requestChannelId, authorDto1, null)
            );

            Instant nextCursor = nowMinus10; // nextCursor은 한 번에 가져온 목록 중 마지막 메시지 생성일자

            PageResponse<MessageDto> expectedResponse = new PageResponse<>(messageDtoList, nextCursor, 5, true, null);

            given(messageService.findAllByChannelId(requestChannelId, cursor, pageable)).willReturn(expectedResponse);

            // when(실행), then(검증)
            mockMvc.perform(get("/api/messages")
                            .param("channelId", requestChannelId.toString())
                            .param("cursor", cursor.toString())
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.nextCursor").value(nextCursor.toString()))
                    .andExpect(jsonPath("$.size").value(messageDtoList.size()))
                    .andExpect(jsonPath("$.hasNext").value(true));
        }

        @Test
        @DisplayName("특정 채널의 메시지 목록을 조회 시 메시지가 하나도 없을 경우 200 상태 코드와 빈 메시지 목록을 반환한다.")
        void success_find_empty_message_list_by_channelId() throws Exception {
            // given(준비)
            UUID requestChannelId = UUID.randomUUID();
            Instant cursor = now;
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

            List<MessageDto> messageDtoList = List.of();

            Instant nextCursor = null; // nextCursor은 한 번에 가져온 목록 중 마지막 메시지 생성일자

            PageResponse<MessageDto> expectedResponse = new PageResponse<>(messageDtoList, nextCursor, pageable.getPageSize(), false, null);

            given(messageService.findAllByChannelId(requestChannelId, cursor, pageable)).willReturn(expectedResponse);

            // when(실행), then(검증)
            mockMvc.perform(get("/api/messages")
                            .param("channelId", requestChannelId.toString())
                            .param("cursor", cursor.toString())
                            .param("page", "0")
                            .param("size", "5")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.nextCursor").isEmpty())
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("메시지 수정 API 테스트")
    class updateMessageByMessageId {

        @Test
        @DisplayName("특정 메시지의 내용을 수정하면 200 상태 코드와 수정된 메시지 정보를 반환한다.")
        void success_update_message_by_id() throws Exception {
            // given(준비)
            BinaryContentDto attachmentDto1 = createBinaryContent("file1", "contentType1", 1L);
            BinaryContentDto attachmentDto2 = createBinaryContent("file2", "contentType2", 2L);
            List<BinaryContentDto> attachments = List.of(attachmentDto1, attachmentDto2);

            UserDto authorDto = createUserDto("test@gmail.com", "test", null, true);
            ChannelDto channelDto = createChannelDto(ChannelType.PUBLIC, "testChannel", "testChannel입니다.", null, Instant.now());

            UUID requestMessageId = UUID.randomUUID();
            MessageUpdateRequest request = new MessageUpdateRequest("updateTestMessageContent");

            Instant createdAt = nowMinus10;
            Instant updatedAt = now;
            MessageDto expectedMessageDto = createMessageDto("updateTestMessageContent", createdAt, updatedAt, channelDto.id(), authorDto, attachments);

            given(messageService.update(requestMessageId, request)).willReturn(expectedMessageDto);

            // when(실행), then(검증)
            mockMvc.perform(patch("/api/messages/{messageId}", requestMessageId)
                            .content(om.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(expectedMessageDto.id().toString()))
                    .andExpect(jsonPath("$.content").value(expectedMessageDto.content()))
                    .andExpect(jsonPath("$.channelId").value(channelDto.id().toString()))
                    .andExpect(jsonPath("$.author.id").value(authorDto.id().toString()));
        }

        @Test
        @DisplayName("메시지를 찾을 수 없으면 404 상태 코드와 MessageNotFoundException 예외 응답을 반환한다.")
        void fail_update_message_by_id_when_message_not_found() throws Exception {
            // given(준비)
            UUID requestMessageId = UUID.randomUUID();
            MessageUpdateRequest request = new MessageUpdateRequest("updateTestMessageContent");

            given(messageService.update(requestMessageId, request)).willThrow(new MessageNotFoundException(requestMessageId));

            // when(실행), then(검증)
            mockMvc.perform(patch("/api/messages/{messageId}", requestMessageId)
                            .content(om.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ErrorCode.MESSAGE_NOT_FOUND.toString()))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.exceptionType").value(MessageNotFoundException.class.getSimpleName()))
                    .andExpect(jsonPath("$.details.messageId").value(requestMessageId.toString()));
        }
    }

    @Nested
    @DisplayName("메시지 삭제 API 테스트")
    class deleteMessageByMessageId {

        @Test
        @DisplayName("특정 메시지를 삭제하면 204 상태 코드를 반환한다.")
        void success_delete_message_by_id() throws Exception {
            // given(준비)
            UUID requestMessageId = UUID.randomUUID();

            // `messageService.delete(requestMessageId)`가 호출되면 아무것도 하지 말라는 로직(delete 반환 타입이 `void`라서)
            willDoNothing().given(messageService).delete(requestMessageId);

            // when(실행), then(검증)
            mockMvc.perform(delete("/api/messages/{messageId}", requestMessageId))
                    .andExpect(status().isNoContent());

            verify(messageService).delete(requestMessageId);
        }

        @Test
        @DisplayName("메시지를 찾지 못하면 404 상태 코드와 MessageNotFoundException 예외 응답을 반환한다.")
        void fail_delete_message_by_id_when_message_not_found() throws Exception {
            // given(준비)
            UUID requestMessageId = UUID.randomUUID();

            willThrow(new MessageNotFoundException(requestMessageId)).given(messageService).delete(requestMessageId);

            // when(실행), then(검증)
            mockMvc.perform(delete("/api/messages/{messageId}", requestMessageId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ErrorCode.MESSAGE_NOT_FOUND.toString()))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.exceptionType").value(MessageNotFoundException.class.getSimpleName()))
                    .andExpect(jsonPath("$.details.messageId").value(requestMessageId.toString()));
        }
    }



}