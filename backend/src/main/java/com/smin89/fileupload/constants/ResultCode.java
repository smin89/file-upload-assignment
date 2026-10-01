package com.smin89.fileupload.constants;

public class ResultCode {

  private ResultCode() {
  }

  public static final String OK = "CO200"; // 성공
  public static final String BAD_REQUEST = "CO400"; // 요청값 에러
  public static final String INTERNAL_SERVER_ERROR = "CO500"; // 최종 Exception 에러

}
