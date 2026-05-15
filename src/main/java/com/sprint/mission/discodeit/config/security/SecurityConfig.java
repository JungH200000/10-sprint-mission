package com.sprint.mission.discodeit.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    // HttpSecurity를 통해 HTTP 요청에 대한 보안 설정 구성
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        SecurityFilterChain chain = http.build();

        log.debug("========== [Spring Security Filter List] ==========");

        return chain;
    }
}
