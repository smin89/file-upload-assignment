package com.smin89.fileupload.constants;

/** 응답 본문의 공통 코드. HTTP 상태와 일대일 대응하지 않는다(예: 409 응답도 CO400). */
public class ResultCode {

  private ResultCode() {
  }

  public static final String OK = "CO200"; // 성공
  public static final String BAD_REQUEST = "CO400"; // 요청값 에러
  public static final String INTERNAL_SERVER_ERROR = "CO500"; // 최종 Exception 에러

}
