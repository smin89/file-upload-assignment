package com.smin89.fileupload.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import com.smin89.fileupload.exception.BusinessException;
import com.smin89.fileupload.dto.SettingDTO;
import com.smin89.fileupload.mapper.SettingMapper;
import com.smin89.fileupload.vo.SettingVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingSvcImpl implements SettingSvc {
  private final SettingMapper settingMapper;

  @Override
  public SettingVO getSettings() {
    var settings = settingMapper.getSettings();
    if (settings == null) {
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 설정이 초기화되지 않았습니다.");
    }
    return settings;
  }

  @Override
  @Transactional
  public int updateSettings(SettingDTO settingDTO) {
    if (settingDTO.getMaxFileCount() < 1 || settingDTO.getMaxFileSize() < 1) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "업로드 제한값은 1 이상이어야 합니다.");
    }
    int updated = settingMapper.updateSettings(settingDTO);
    // 드라이버가 실제 변경 행 수를 반환하는 경우 동일 값 재요청도 성공으로 처리한다.
    if (updated == 0) {
      var current = getSettings();
      if (current.getMaxFileCount() == settingDTO.getMaxFileCount()
          && current.getMaxFileSize() == settingDTO.getMaxFileSize()) return 0;
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 설정 변경에 실패했습니다.");
    }
    if (updated != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 설정 변경에 실패했습니다.");
    return updated;
  }
}
