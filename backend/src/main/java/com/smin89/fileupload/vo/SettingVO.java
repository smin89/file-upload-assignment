package com.smin89.fileupload.vo;

import java.time.LocalDateTime;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/** upload_settings 조회용 객체. */
@Data
@Alias("settingVO")
public class SettingVO {
  private Long id;
  private int maxFileCount;
  private long maxFileSize;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
