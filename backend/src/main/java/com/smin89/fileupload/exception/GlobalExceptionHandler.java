package com.smin89.fileupload.exception;

import com.smin89.fileupload.constants.ResultCode;
import com.smin89.fileupload.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** MVC 요청 오류와 처리 중 발생한 예외를 공통 응답 형식으로 변환한다. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Object> handleBusinessException(BusinessException ex) {
    boolean serverError = ex.getStatus().is5xxServerError();
    if (serverError) {
      log.error("업무 처리 실패", ex);
    }
    ResultDTO<Object> result = ResultDTO.res(
        serverError ? ResultCode.INTERNAL_SERVER_ERROR : ResultCode.BAD_REQUEST,
        ex.getMessage(), ex.getData());
    return ResponseEntity.status(ex.getStatus()).body(result);
  }

  // Spring이 결정한 상태 코드와 Allow 등의 응답 헤더는 유지한다.
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    boolean serverError = statusCode.is5xxServerError();
    if (serverError) {
      log.error("API 요청 처리 실패", ex);
    }

    ResultDTO<Void> result = ResultDTO.res(
        serverError ? ResultCode.INTERNAL_SERVER_ERROR : ResultCode.BAD_REQUEST,
        messageFor(statusCode.value()));

    return super.handleExceptionInternal(ex, result, headers, statusCode, request);
  }

  // DB 오류를 포함한 예상하지 못한 예외의 원문은 서버 로그에만 기록한다.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
    return handleExceptionInternal(
        ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  private String messageFor(int status) {
    return switch (status) {
      case 400 -> "요청 형식 또는 입력값을 확인해주세요.";
      case 404 -> "요청한 리소스를 찾을 수 없습니다.";
      case 405 -> "지원하지 않는 HTTP 메서드입니다.";
      case 406 -> "요청한 응답 형식을 지원하지 않습니다.";
      case 409 -> "현재 데이터와 충돌하는 요청입니다.";
      case 413 -> "허용된 업로드 크기를 초과했습니다.";
      case 415 -> "지원하지 않는 요청 Content-Type입니다.";
      default -> status >= 500 ? "서버 오류가 발생했습니다." : "요청을 처리할 수 없습니다.";
    };
  }
}
