package com.sprint.mission.discodeit.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public class ErrorResponse {
    private Instant timestamp;
    private String code;
    private String message;
    Map<String, Object> details;
    String exceptionType; // 발생한 예외의 클래스 이름
    int status; // HTTP 상태코드

    public ErrorResponse(DiscodeitException e, int status) {
        this.timestamp = e.getTimestamp();
        this.code = e.getErrorCode().name();
        this.message = e.getMessage();
        this.details = e.getDetails();
        this.exceptionType = e.getClass().getSimpleName(); // 예외 발생 파일 이름
        this.status = status;
    }

    public ErrorResponse(Exception e, int status) {
        this.timestamp = Instant.now();
        this.code = e.getClass().getSimpleName(); // 예외 발생 파일 이름
        this.message = e.getMessage();
        this.details = new HashMap<>();
        this.exceptionType = e.getClass().getSimpleName();
        this.status = status;
    }
}
