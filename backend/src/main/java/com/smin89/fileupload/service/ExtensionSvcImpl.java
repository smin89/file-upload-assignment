package com.smin89.fileupload.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import static com.smin89.fileupload.constants.ResultCode.*;
import com.smin89.fileupload.constants.CommonConstants;
import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.exception.BusinessException;
import com.smin89.fileupload.mapper.ExtensionMapper;
import com.smin89.fileupload.vo.ExtensionVO;
import lombok.RequiredArgsConstructor;

/** 업무 검증과 DB 변경을 하나의 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class ExtensionSvcImpl implements ExtensionSvc {
  private final ExtensionMapper extensionsMapper;

  @Override
  public List<ExtensionVO> getExtensionList() {
    return extensionsMapper.getExtensionList();
  }

  @Override
  @Transactional
  public void updateFixedExtension(ExtensionDTO command) {
    var existing = requireForUpdate(command.getId());
    if (!"FIXED".equals(existing.getExtensionType())) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "고정 확장자만 변경할 수 있습니다.", null);
    }
    // 같은 값의 재요청은 DB 변경 없이 성공으로 처리한다.
    if (existing.isEnabled() == command.isEnabled()) {
      return;
    }
    requireChanged(extensionsMapper.updateFixedExtension(command), "확장자 상태 변경에 실패했습니다.");
  }

  @Override
  @Transactional
  public void regCustomExtension(String input) {
    String extension = normalize(input);
    // 모든 커스텀 등록·삭제가 같은 singleton 행을 먼저 잠가 개수 검사와 INSERT를 직렬화한다.
    lockCustomPolicy();
    if (extensionsMapper.getExtensionByName(extension) != null) throw duplicate();
    if (extensionsMapper.countCustomExtensions() >= CommonConstants.MAX_EXT_COUNT) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, EXTENSION_LIMIT_EXCEEDED, "커스텀 확장자는 최대 %d개까지 등록할 수 있습니다.".formatted(CommonConstants.MAX_EXT_COUNT), null);
    }
    try {
      requireChanged(extensionsMapper.regCustomExtension(extension), "확장자 등록에 실패했습니다.");
    } catch (DuplicateKeyException ex) {
      // 사전 조회 외에도 DB UNIQUE 제약을 최종 중복 방어 수단으로 사용한다.
      throw duplicate();
    }
  }

  @Override
  @Transactional
  public void deleteCustomExtension(long id) {
    validateId(id);
    lockCustomPolicy();
    var existing = requireForUpdate(id);
    if (!"CUSTOM".equals(existing.getExtensionType())) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "커스텀 확장자만 삭제할 수 있습니다.", null);
    }
    requireChanged(extensionsMapper.deleteCustomExtension(id), "확장자 삭제에 실패했습니다.");
  }

  private ExtensionVO requireForUpdate(long id) {
    validateId(id);
    var row = extensionsMapper.getExtensionForUpdate(id);
    if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, EXTENSION_NOT_FOUND, "존재하지 않는 확장자입니다.", null);
    return row;
  }

  private void validateId(long id) {
    if (id < 1) throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "올바른 확장자 ID가 필요합니다.", null);
  }

  private void lockCustomPolicy() {
    // 같은 트랜잭션에서 누락된 초기 행을 복구한다. 기본값은 DB 스키마를 따른다.
    extensionsMapper.ensureCustomPolicy();
    if (extensionsMapper.lockCustomPolicy() == null) {
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "정책 설정을 확인할 수 없습니다.");
    }
  }

  private void requireChanged(int count, String message) {
    if (count != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  private BusinessException duplicate() {
    return new BusinessException(HttpStatus.CONFLICT, DUPLICATE_EXTENSION, "이미 등록된 확장자입니다.", null);
  }

  private String normalize(String input) {
    if (input == null) throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "확장자를 입력해주세요.", null);
    String value = input.strip().toLowerCase(Locale.ROOT);
    if (value.startsWith(".")) value = value.substring(1);
    if (value.length() > CommonConstants.EXT_NAME_LIMIT) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "확장자 명은 %d자를 넘을 수 없습니다.".formatted(CommonConstants.EXT_NAME_LIMIT), null);
    }
    if (!value.matches("[a-z0-9]+")) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, INVALID_EXTENSION, "확장자는 영문과 숫자만 사용할 수 있습니다.", null);
    }
    return value;
  }
}
