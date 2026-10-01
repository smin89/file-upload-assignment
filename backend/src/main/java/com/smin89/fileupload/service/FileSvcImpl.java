package com.smin89.fileupload.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import static com.smin89.fileupload.constants.ResultCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import com.smin89.fileupload.dto.FileDTO;
import com.smin89.fileupload.exception.BusinessException;
import com.smin89.fileupload.mapper.FileMapper;
import com.smin89.fileupload.storage.LocalFileStorage;
import com.smin89.fileupload.vo.FileVO;
import com.smin89.fileupload.vo.SettingVO;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSvcImpl implements FileSvc {
  private final FileMapper fileMapper;
  private final SettingSvc settingSvc;
  private final ExtensionSvc extensionSvc;
  private final LocalFileStorage storage;

  @Override
  @Transactional
  public FileDTO.UploadResult uploadFiles(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) throw badRequest(MISSING_FILE, "업로드 파일이 필요합니다.");
    var settings = settingSvc.getSettings();
    if (settings.getMaxFileCount() < 1 || settings.getMaxFileSize() < 1) {
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 설정이 올바르지 않습니다.");
    }
    if (files.size() > settings.getMaxFileCount()) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, FILE_COUNT_EXCEEDED, "한 번에 업로드할 수 있는 파일 개수를 초과했습니다.",
          Map.of("maxFileCount", settings.getMaxFileCount(), "requestedFileCount", files.size()));
    }
    log.debug("파일 업로드 검증: fileCount={}, maxFileCount={}, maxFileSize={}",
        files.size(), settings.getMaxFileCount(), settings.getMaxFileSize());
    // 요청 시작 시 조회한 정책을 모든 파일에 동일하게 적용한다.
    Set<String> blocked = new HashSet<>();
    extensionSvc.getExtensionList().stream()
        .filter(e -> "CUSTOM".equals(e.getExtensionType()) || e.isEnabled())
        .forEach(e -> blocked.add(e.getExtension().toLowerCase(Locale.ROOT)));
    for (var file : files) validate(file, settings, blocked);

    // 검증을 전부 통과한 후에만 저장한다. DB 커밋 실패도 포함해 롤백 시 실제 파일을 정리한다.
    List<String> storedNames = new ArrayList<>();
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          long cleaned = storedNames.stream().filter(storage::removeQuietly).count();
          log.warn("파일 업로드 롤백: 정리 대상={}, 정리 성공={}, 정리 실패={}",
              storedNames.size(), cleaned, storedNames.size() - cleaned);
        } else if (status == STATUS_COMMITTED) {
          log.info("파일 업로드 완료: {}개", storedNames.size());
        } else {
          // 커밋 결과 불명 시 DB에 기록됐을 수 있으므로 자동 삭제 대신 운영 확인이 필요하다.
          log.error("업로드 트랜잭션 결과 불명. 저장 파일 확인 필요: {}", storedNames);
        }
      }
    });
    List<FileVO> saved = new ArrayList<>();
    for (var file : files) {
      var metadata = storage.save(file, settings.getMaxFileSize(), storedNames);
      if (fileMapper.insertFile(metadata) != 1) {
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 정보 저장에 실패했습니다.");
      }
      saved.add(metadata);
    }
    return new FileDTO.UploadResult(saved.stream().map(FileDTO.Uploaded::from).toList());
  }

  private void validate(MultipartFile file, SettingVO settings, Set<String> blocked) {
    if (file == null) throw badRequest(MISSING_FILE, "업로드 파일이 필요합니다.");
    if (file.isEmpty()) throw badRequest(EMPTY_FILE, "0 byte 파일은 업로드할 수 없습니다.");
    String name = file.getOriginalFilename();
    if (file.getSize() > settings.getMaxFileSize()) {
      throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, FILE_SIZE_EXCEEDED, "허용된 파일 크기를 초과했습니다.",
          Map.of("fileName", name == null ? "" : name, "maxFileSize", settings.getMaxFileSize(), "fileSize", file.getSize()));
    }
    if (name == null || name.isBlank() || name.length() > 255 || name.equals(".") || name.equals("..")
        || name.endsWith(".") || !name.equals(name.strip())
        || name.chars().anyMatch(c -> Character.isISOControl(c) || "/\\:".indexOf(c) >= 0)) {
      throw badRequest(INVALID_FILE_NAME, "파일명에 경로 또는 허용되지 않는 문자가 포함되어 있습니다.");
    }
    String[] parts = name.toLowerCase(Locale.ROOT).split("\\.", -1);
    // .env는 확장자가 없고, .env.exe는 exe를 검사한다.
    for (int i = name.startsWith(".") ? 2 : 1; i < parts.length; i++) {
      if (blocked.contains(parts[i])) {
        log.info("차단 확장자 업로드 거부: {}", parts[i]);
        throw new BusinessException(HttpStatus.BAD_REQUEST, BLOCKED_FILE_EXTENSION, "%s 파일은 차단된 %s 확장자이므로 업로드할 수 없습니다.".formatted(name, parts[i]),
            Map.of("fileName", name, "extension", parts[i]));
      }
    }
  }

  private BusinessException badRequest(String code, String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, code, message, null);
  }
}
