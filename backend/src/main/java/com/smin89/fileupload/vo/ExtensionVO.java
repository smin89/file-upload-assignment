package com.smin89.fileupload.vo;

import java.time.LocalDateTime;

import org.apache.ibatis.type.Alias;

import lombok.Data;

/** file_extensions 조회용 객체. */
@Data
@Alias("extensionsVO")
public class ExtensionVO {
  private Long id;
  private String extension;
  private String extensionType; // FIXED 또는 CUSTOM
  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
