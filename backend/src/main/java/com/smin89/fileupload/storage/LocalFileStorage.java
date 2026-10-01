package com.smin89.fileupload.storage;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import static com.smin89.fileupload.constants.ResultCode.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.smin89.fileupload.exception.BusinessException;
import com.smin89.fileupload.vo.FileVO;

/** 파일 I/O만 담당하며 업무 정책과 DB 처리는 서비스에서 수행한다. */
@Component
@Slf4j
public class LocalFileStorage {
  private final Path root;
  public LocalFileStorage(@Value("${app.upload.directory:./data/uploads}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize();
  }

  public FileVO save(MultipartFile file, long maxSize, List<String> storedNames) {
    String storedName = UUID.randomUUID().toString();
    Path target = root.resolve(storedName);
    try {
      Files.createDirectories(root);
      var digest = MessageDigest.getInstance("SHA-256");
      long size = 0;
      // CREATE_NEW로 기존 파일을 덮어쓰지 않고 원본 파일명은 경로에 사용하지 않는다.
      try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
           InputStream input = trackAndOpen(file, storedName, storedNames)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
          size += read;
          if (size > maxSize) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, FILE_SIZE_EXCEEDED, "허용된 파일 크기를 초과했습니다.", null);
          output.write(buffer, 0, read);
          digest.update(buffer, 0, read);
        }
      }
      if (size == 0) throw new BusinessException(HttpStatus.BAD_REQUEST, EMPTY_FILE, "0 byte 파일은 업로드할 수 없습니다.", null);
      var metadata = new FileVO();
      metadata.setOriginalName(file.getOriginalFilename());
      metadata.setStoredName(storedName);
      metadata.setStoragePath(target.toString());
      // 파일 내용 판별을 구현하지 않으므로 클라이언트 Content-Type 대신 안전한 기본값을 사용한다.
      metadata.setMimeType("application/octet-stream");
      metadata.setSizeBytes(size);
      metadata.setSha256(HexFormat.of().formatHex(digest.digest()));
      metadata.setCreatedAt(LocalDateTime.now().withNano(0));
      return metadata;
    } catch (IOException | NoSuchAlgorithmException ex) {
      log.error("파일 저장 실패", ex);
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
    }
  }

  private InputStream trackAndOpen(MultipartFile file, String name, List<String> storedNames) throws IOException {
    // 입력 스트림 열기 실패나 부분 쓰기도 롤백 정리 대상에 포함한다.
    storedNames.add(name);
    return file.getInputStream();
  }

  /** 파일이 이미 없으면 정리 성공으로 취급하며, 실패는 로그와 반환값으로 알린다. */
  public boolean removeQuietly(String name) {
    try {
      Files.deleteIfExists(root.resolve(name));
      return true;
    } catch (IOException ex) {
      log.error("롤백 파일 정리 실패: {}", name, ex);
      return false;
    }
  }
}
