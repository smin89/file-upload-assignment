package com.smin89.fileupload.exception;

import java.util.Objects;
import com.smin89.fileupload.constants.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 예상 가능한 업무 오류. message와 data에는 클라이언트에 공개할 정보만 전달한다. */
@Getter
public class BusinessException extends RuntimeException {
  // 전역 예외 처리기가 실제 HTTP 응답 상태와 공통 결과 코드를 결정할 때 사용한다.
  private final HttpStatus status;
  private final String resultCode;
  // 파일명·제한값 등 공개 가능한 오류 부가 정보. 응답의 data에 그대로 전달된다.
  private final Object data;

  public BusinessException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public BusinessException(HttpStatus status, String message, Object data) {
    this(status, status.is5xxServerError() ? ResultCode.INTERNAL_SERVER_ERROR : ResultCode.BAD_REQUEST, message, data);
  }

  public BusinessException(HttpStatus status, String resultCode, String message, Object data) {
    super(Objects.requireNonNull(message, "message"));
    this.resultCode = Objects.requireNonNull(resultCode, "resultCode");
    this.status = Objects.requireNonNull(status, "status");
    // 성공·리다이렉션 상태가 업무 오류 응답으로 사용되는 것을 방지한다.
    if (!status.is4xxClientError() && !status.is5xxServerError()) {
      throw new IllegalArgumentException("업무 예외는 4xx 또는 5xx 상태여야 합니다.");
    }
    this.data = data;
  }
}
