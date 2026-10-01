package com.smin89.fileupload.utils;

import org.springframework.stereotype.Component;

@Component
public class CommonUtils {
  // String 값 NULL 또는 "" 체크
  public static boolean isStringEmpty(String str) {
    return str == null || str.isBlank();
  }
}
