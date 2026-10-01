package com.smin89.fileupload.vo;

import java.time.LocalDateTime;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("fileVO")
public class FileVO {
  private Long id;
  private String originalName;
  private String storedName;
  private String storagePath;
  private String mimeType;
  private long sizeBytes;
  private String sha256;
  private LocalDateTime createdAt;
}
