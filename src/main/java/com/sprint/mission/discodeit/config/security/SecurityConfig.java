package com.sprint.mission.discodeit.config.security;

import com.sprint.mission.discodeit.security.handler.LoginFailureHandler;
import com.sprint.mission.discodeit.security.handler.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableMethodSecurity // Method Security 활성화
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    // SecurityFilterChain Bean 등록
    // HttpSecurity를 통해 HTTP 요청에 대한 보안 설정 구성
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                )
                .authorizeHttpRequests(auth -> auth
                        // Auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf-token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // 그 외 나머지 `/api/**` 요청
                        .requestMatchers("/api/**").authenticated()
                        // `/api/**` 가 아닌 요청 (`/swagger-ui.html`, `/actuator/**` 등)
                        .anyRequest().permitAll()
                )
        ;

        SecurityFilterChain chain = http.build();

        log.debug("========== [Spring Security Filter List - START] ==========");
        chain.getFilters().forEach(filter ->
                log.debug("{}", filter.getClass().getSimpleName())
        );
        log.debug("========== [Spring Security Filter List - END] ==========");

        return chain;
    }

    // PasswordEncoder Bean 등록
    // 비밀번호를 bcrypt 알고리즘으로 해시 처리 - 같은 비밀번호라도 다른 해시값 생성
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
