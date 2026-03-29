package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.ChannelDto;
import com.sprint.mission.discodeit.dto.channel.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.channel.PrivateChannelParticipantRequiredException;
import com.sprint.mission.discodeit.exception.common.InvalidInputException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadStatusRepository readStatusRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChannelMapper channelMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private BasicChannelService basicChannelService;

    private String name;
    private String description;

    @BeforeEach
    void setup() {
        name = "testChannel";
        description = "test channel 입니다.";
    }

    @Nested
    @DisplayName("공개 채널 생성 테스트")
    class createPublicChannel {

        @Test
        @DisplayName("공개 채널을 생성할 수 있다.")
        void success_create_public_channel() {
            // given(준비)
            PublicChannelCreateRequest request = new PublicChannelCreateRequest(name, description);

            UUID channelId = UUID.randomUUID();
            Channel channel = new Channel(ChannelType.PUBLIC, name, description);
            ReflectionTestUtils.setField(channel, "id", channelId);
            ChannelDto expectedChannelDto = new ChannelDto(channelId, ChannelType.PUBLIC, name, description, List.of(), null);

            given(channelMapper.toDto(any(Channel.class))).willReturn(expectedChannelDto);

            // when(실행)
            ChannelDto result = basicChannelService.createPublicChannel(request);

            // then(검증)
            assertEquals(expectedChannelDto, result);
            assertEquals(expectedChannelDto.type(), result.type());
            assertEquals(expectedChannelDto.name(), result.name());
            assertEquals(expectedChannelDto.description(), result.description());

            verify(channelRepository).save(any(Channel.class));
            verify(channelMapper).toDto(any(Channel.class));
        }
    }

    @Nested
    @DisplayName("비공개 채널 생성 테스트")
    class createPrivateChannel {
        private User user1;
        private User user2;

        @BeforeEach
        void setupPrivateChannel() {
            user1 = new User("test1@gmail.com", "test1", "1234", null);
            user2 = new User("test2@gmail.com", "test2", "1234", null);
        }

        @Test
        @DisplayName("비공개 채널을 생성할 수 있다.")
        void success_create_private_channel() {
            // given(준비)
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            ReflectionTestUtils.setField(user1, "id", userId1);
            ReflectionTestUtils.setField(user2, "id", userId2);
            UserDto userDto1 = new UserDto(userId1, user1.getUsername(), user1.getEmail(), null, false);
            UserDto userDto2 = new UserDto(userId2, user2.getUsername(), user2.getEmail(), null, false);

            List<UUID> participantIds = List.of(userId1, userId2);
            PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);

            UUID channelId = UUID.randomUUID();
            Channel channel = new Channel(ChannelType.PRIVATE, null, null);
            ReflectionTestUtils.setField(channel, "id", channelId);
            ChannelDto expectedChannelDto = new ChannelDto(channelId, ChannelType.PRIVATE, null, null, List.of(userDto1, userDto2), null);

            given(userRepository.findById(userId1)).willReturn(Optional.of(user1));
            given(userRepository.findById(userId2)).willReturn(Optional.of(user2));
            given(channelMapper.toDto(any(Channel.class))).willReturn(expectedChannelDto);

            // when(실행)
            ChannelDto result = basicChannelService.createPrivateChannel(request);

            // then(검증)
            assertEquals(expectedChannelDto, result);
            assertEquals(expectedChannelDto.type(), result.type());

            verify(userRepository, times(2)).findById(any());
            verify(channelRepository).save(any(Channel.class));
            verify(readStatusRepository, times(2)).save(any(ReadStatus.class));
            verify(channelMapper).toDto(any(Channel.class));
        }

        @Test
        @DisplayName("비공개 채널에 참가자가 없으면 예외가 발생한다.")
        void fail_create_private_channel_when_no_participants() {
            // given(준비)
            PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(List.of());

            // when(실행), then(검증)
            assertThrows(PrivateChannelParticipantRequiredException.class,
                    () -> basicChannelService.createPrivateChannel(request));

            verify(userRepository, never()).findById(any());
            verify(channelRepository, never()).save(any(Channel.class));
            verify(readStatusRepository, never()).save(any(ReadStatus.class));
            verify(channelMapper, never()).toDto(any(Channel.class));
        }

        @Test
        @DisplayName("참가자의 ID가 null이면 예외가 발생한다.")
        void fail_create_private_channel_when_participantId_null() {
            // given(준비)
            List<UUID> participantIds = new ArrayList<>(Arrays.asList(null, null));
            PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);

            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicChannelService.createPrivateChannel(request));

            verify(userRepository, never()).findById(any());
            verify(channelRepository, never()).save(any(Channel.class));
            verify(readStatusRepository, never()).save(any(ReadStatus.class));
            verify(channelMapper, never()).toDto(any(Channel.class));
        }

        @Test
        @DisplayName("해당 ID를 가진 참가자를 찾을 수 없으면 예외가 발생한다.")
        void fail_create_private_channel_when_participant_not_found() {
            // given(준비)
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            ReflectionTestUtils.setField(user1, "id", userId1);
            ReflectionTestUtils.setField(user2, "id", userId2);

            List<UUID> participantIds = List.of(userId1, userId2);
            PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);

            given(userRepository.findById(any())).willReturn(Optional.empty());

            // when(실행), then(검증)
            assertThrows(UserNotFoundException.class,
                    () -> basicChannelService.createPrivateChannel(request));

            verify(userRepository).findById(any());
            verify(channelRepository, never()).save(any(Channel.class));
            verify(readStatusRepository, never()).save(any(ReadStatus.class));
            verify(channelMapper, never()).toDto(any(Channel.class));
        }
    }


    @Test
    void find() {
        // given(준비)

        // when(실행)

        // then(검증)
    }

    @Test
    void findAllByUserId() {
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

    @Nested
    @DisplayName("채널 삭제 테스트")
    class deleteChannel {
        UUID channelId;

        @BeforeEach
        void setupDeleteChannel() {
            channelId = UUID.randomUUID();
        }

        @Test
        @DisplayName("채널을 삭제할 수 있다.")
        void success_delete_channel() {
            // given(준비)
            Channel channel = new Channel(ChannelType.PUBLIC, name, description);
            ReflectionTestUtils.setField(channel, "id", channelId);

            given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

            // when(실행)
            basicChannelService.delete(channelId);

            // then(검증)
            verify(channelRepository).findById(channelId);
            verify(channelRepository).deleteById(channelId);
        }

        @Test
        @DisplayName("채널 ID가 null이면 예외가 발생한다.")
        void fail_delete_channel_when_channelId_null() {
            // when(실행), then(검증)
            assertThrows(InvalidInputException.class,
                    () -> basicChannelService.delete(null));

            verify(channelRepository, never()).findById(any());
            verify(channelRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("해당 ID를 가진 채널이 없으면 예외가 발생한다.")
        void fail_delete_channel_when_channel_not_found() {
            // given(준비)
            given(channelRepository.findById(channelId)).willReturn(Optional.empty());

            // when(실행), then(검증)
            assertThrows(ChannelNotFoundException.class,
                    () -> basicChannelService.delete(channelId));

            verify(channelRepository).findById(channelId);
            verify(channelRepository, never()).deleteById(channelId);
        }
    }
}