package com.smin89.fileupload.constants;

/** 공통 응답 코드 및 예측 가능한 업무 오류 코드. */
public class ResultCode {

  private ResultCode() {
  }

  public static final String OK = "CO200"; // 성공
  public static final String BAD_REQUEST = "CO400"; // 요청값 에러
  public static final String INTERNAL_SERVER_ERROR = "CO500"; // 최종 Exception 에러

  public static final String DUPLICATE_EXTENSION = "DUPLICATE_EXTENSION";
  public static final String EXTENSION_LIMIT_EXCEEDED = "EXTENSION_LIMIT_EXCEEDED";
  public static final String INVALID_EXTENSION = "INVALID_EXTENSION";
  public static final String EXTENSION_NOT_FOUND = "EXTENSION_NOT_FOUND";
  public static final String BLOCKED_FILE_EXTENSION = "BLOCKED_FILE_EXTENSION";
  public static final String FILE_SIZE_EXCEEDED = "FILE_SIZE_EXCEEDED";
  public static final String FILE_COUNT_EXCEEDED = "FILE_COUNT_EXCEEDED";
  public static final String EMPTY_FILE = "EMPTY_FILE";
  public static final String MISSING_FILE = "MISSING_FILE";
  public static final String INVALID_FILE_NAME = "INVALID_FILE_NAME";
  public static final String INVALID_UPLOAD_SETTING = "INVALID_UPLOAD_SETTING";

}
