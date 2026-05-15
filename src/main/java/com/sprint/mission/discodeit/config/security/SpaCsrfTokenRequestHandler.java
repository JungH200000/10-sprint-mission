package com.sprint.mission.discodeit.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler csrf = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xorCsrf = new XorCsrfTokenRequestAttributeHandler();

    @Override
    // 클라이언트가 요청을 담아 보낸 CSRF 토큰 값을 꺼내는 메서드
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        // CSRF 토큰이 기대하는 요청 헤더 이름이 있는지 확인
        String header = request.getHeader(csrfToken.getHeaderName());

        // CSR/SPA 요청인지 판단
        // 없으면 xor handler 사용
        CsrfTokenRequestHandler csrfTokenRequestHandler = StringUtils.hasText(header)
                ? this.csrf
                : this.xorCsrf;

        // handler를 통해 요청에서 CSRF 토큰을 꺼내고, 저장소에 있는 토큰과 비교
        return csrfTokenRequestHandler.resolveCsrfTokenValue(request, csrfToken);
    }

    @Override
    // CSRF 토큰을 요청 처리 과정에서 사용할 수 있도록 준비하는 메서드
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken
    ) {
        // request attribute에 CSRF 토큰 노출 시 기본적으로 xor handler 사용
        this.xorCsrf.handle(request, response, csrfToken);

        // 지연 로딩된 토큰을 강제 로드
        csrfToken.get();
    }
}
