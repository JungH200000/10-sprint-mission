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
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        String header = request.getHeader(csrfToken.getHeaderName());

        CsrfTokenRequestHandler csrfTokenRequestHandler = StringUtils.hasText(header)
                ? this.csrf
                : this.xorCsrf;

        return csrfTokenRequestHandler.resolveCsrfTokenValue(request, csrfToken);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken
    ) {

        this.xorCsrf.handle(request, response, csrfToken);

        // 지연 로딩된 토큰을 강제 로드
        csrfToken.get();
    }
}
