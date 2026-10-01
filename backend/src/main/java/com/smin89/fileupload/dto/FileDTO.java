package com.smin89.fileupload.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.smin89.fileupload.vo.FileVO;

/** 저장 경로와 서버 저장명은 외부 응답에 노출하지 않는다. */
public final class FileDTO {
  private FileDTO() {}
  public record Uploaded(Long id, String originalName, String mimeType, long sizeBytes, LocalDateTime createdAt) {
    public static Uploaded from(FileVO file) {
      return new Uploaded(file.getId(), file.getOriginalName(), file.getMimeType(), file.getSizeBytes(), file.getCreatedAt());
    }
  }
  public record UploadResult(List<Uploaded> files) {}
}
