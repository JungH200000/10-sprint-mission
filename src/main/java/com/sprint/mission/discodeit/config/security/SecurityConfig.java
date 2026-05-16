package com.sprint.mission.discodeit.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    // SecurityFilterChain Bean 등록
    // HttpSecurity를 통해 HTTP 요청에 대한 보안 설정 구성
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));

        SecurityFilterChain chain = http.build();

        log.debug("========== [Spring Security Filter List - START] ==========");
        chain.getFilters().forEach(filter ->
                log.debug("{}", filter.getClass().getSimpleName())
        );
        log.debug("========== [Spring Security Filter List - END] ==========");

        return chain;
    }

    @Bean
    // PasswordEncoder Bean 등록
    // 비밀번호를 bcrypt 알고리즘으로 해시 처리 - 같은 비밀번호라도 다른 해시값 생성
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
