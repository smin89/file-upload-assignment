package com.smin89.fileupload.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.smin89.fileupload.dto.SettingDTO;
import com.smin89.fileupload.vo.SettingVO;

@Mapper
public interface SettingMapper {
  // 설정 값 조회
  SettingVO getSettings();

  // 설정 값 변경
  int updateSettings(SettingDTO settingDTO);
}
