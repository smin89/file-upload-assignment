package com.smin89.fileupload.exception;

import java.util.Objects;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 예상 가능한 업무 오류. message와 data에는 클라이언트에 공개할 정보만 전달한다. */
@Getter
public class BusinessException extends RuntimeException {
  private final HttpStatus status;
  private final Object data;

  public BusinessException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public BusinessException(HttpStatus status, String message, Object data) {
    super(Objects.requireNonNull(message, "message"));
    this.status = Objects.requireNonNull(status, "status");
    if (!status.is4xxClientError() && !status.is5xxServerError()) {
      throw new IllegalArgumentException("업무 예외는 4xx 또는 5xx 상태여야 합니다.");
    }
    this.data = data;
  }
}
