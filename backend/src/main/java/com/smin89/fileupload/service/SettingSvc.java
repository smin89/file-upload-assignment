package com.smin89.fileupload.service;

import com.smin89.fileupload.dto.SettingDTO;
import com.smin89.fileupload.vo.SettingVO;

/** 업로드 설정 값 조회 및 수정 서비스 */
public interface SettingSvc {
  // 설정 값 조회
  SettingVO getSettings();

  // 설정 값 변경
  int updateSettings(SettingDTO settingDTO);
}
