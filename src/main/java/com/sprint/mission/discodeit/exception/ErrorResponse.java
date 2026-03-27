package com.sprint.mission.discodeit.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private Instant timestamp;
    private String code;
    private String message;
    Map<String, Object> details;
    String exceptionType; // 발생한 예외의 클래스 이름
    int status; // HTTP 상태코드
}
