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
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** MVC 요청 오류와 처리 중 발생한 예외를 공통 응답 형식으로 변환한다. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  // 업무 예외는 호출부에서 공개용으로 작성한 메시지와 부가 정보를 그대로 전달한다.
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {
    boolean serverError = ex.getStatus().is5xxServerError();
    if (serverError) {
      log.error("업무 처리 실패", ex);
    } else {
      // 예상 가능한 요청 오류는 스택 트레이스와 사용자 입력 없이 상태만 기록한다.
      log.debug("업무 요청 거부: status={}", ex.getStatus().value());
    }
    logUploadFailure(request, ex.getStatus().value(), ex.getResultCode());
    ResultDTO<Object> result = ResultDTO.res(
        ex.getResultCode(),
        ex.getMessage(), ex.getData());
    return ResponseEntity.status(ex.getStatus()).body(result);
  }

  // Spring MVC 오류의 본문을 공통 DTO로 바꾸되 상태 코드와 Allow 등의 헤더는 유지한다.
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    boolean serverError = statusCode.is5xxServerError();
    if (serverError) {
      log.error("API 요청 처리 실패", ex);
    }

    String code = serverError ? ResultCode.INTERNAL_SERVER_ERROR
        : statusCode.value() == 413 ? ResultCode.FILE_SIZE_EXCEEDED : ResultCode.BAD_REQUEST;
    logUploadFailure(request, statusCode.value(), code);
    ResultDTO<Void> result = ResultDTO.res(
        code,
        messageFor(statusCode.value()));

    return super.handleExceptionInternal(ex, result, headers, statusCode, request);
  }

  // DB 오류를 포함한 예상하지 못한 예외의 원문은 서버 로그에만 기록한다.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
    return handleExceptionInternal(
        ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  // 사용자 파일명/경로/예외 원문 없이 HTTP 업로드 실패를 INFO 수준에서 기록한다.
  private void logUploadFailure(WebRequest request, int status, String code) {
    if (request instanceof ServletWebRequest servlet) {
      var http = servlet.getRequest();
      if ("POST".equals(http.getMethod())
          && (http.getContextPath() + "/files").equals(http.getRequestURI())) {
        log.info("파일 업로드 실패: status={}, code={}", status, code);
      }
    }
  }

  // 일반 예외는 내부 메시지 대신 HTTP 상태별 공개 메시지로 응답한다.
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
