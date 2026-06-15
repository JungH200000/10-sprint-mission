package com.sprint.mission.discodeit.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// WebSocket/STOMP 연결 엔드포인트와 메시지 브로커 prefix 설정 클래스
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 클라이언트가 최초 WebSocket/STOMP 연결을 맺을 엔드포인트를 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

    }

    // 구독용 prefix(/sub)와 발행용 prefix(/pub)를 메시지 브로커에 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메모리 기반 SimpleBroker 사용
        // /sub으로 시작하는 API(목적지)를 SimpleBroker가 처리
        registry.enableSimpleBroker("/sub");
        // Destination Prefix를 /pub으로 설정
        // 서버의 Controller로 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
