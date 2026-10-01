package com.smin89.fileupload.service;

import java.util.List;

import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.vo.ExtensionVO;

/** 확장자 정책 기능을 제공하는 서비스 계약. */
public interface ExtensionSvc {
  /** 고정·커스텀 정책을 구분한 조회 응답을 반환한다. */
  List<ExtensionVO> getExtensionList();

  /** 고정·커스텀 정책을 구분한 조회 응답을 반환한다. */
  ExtensionVO getExtension(long id);

  /** 고정 확장자 사용 여부 변경 */
  int updateFixedExtension(ExtensionDTO extensionsDTO);

  /** 확장자 명으로 확장자 조회 (중복 체크) */
  ExtensionVO getExtensionByName(String extension);

  /** 커스텀 확장자 등록 */
  int regCustomExtension(String extension);

  /** 커스텀 확장자 삭제 */
  int deleteCustomExtension(long id);
}
