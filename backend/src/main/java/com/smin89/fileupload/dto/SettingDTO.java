package com.smin89.fileupload.dto;

import org.apache.ibatis.type.Alias;

import lombok.Data;
import jakarta.validation.constraints.Positive;

@Data
@Alias("settingDTO")
public class SettingDTO {
  @Positive(message = "최대 파일 개수는 1 이상이어야 합니다.")
  private int maxFileCount;
  @Positive(message = "최대 파일 크기는 1 byte 이상이어야 합니다.")
  private long maxFileSize;
  /** 조회·변경 모두 같은 공개 응답 필드를 사용한다. */
  public record Response(int maxFileCount, long maxFileSize) {}
}
