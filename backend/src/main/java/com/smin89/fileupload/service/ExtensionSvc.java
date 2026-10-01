package com.smin89.fileupload.service;

import java.util.List;

import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.vo.ExtensionVO;

/** 확장자 정책 기능을 제공하는 서비스 계약. */
public interface ExtensionSvc {
  /** DB 조회용 전체 목록을 반환한다. API 응답 필드 구성은 Controller가 담당한다. */
  List<ExtensionVO> getExtensionList();

  /** 고정 확장자 사용 여부 변경 */
  void updateFixedExtension(ExtensionDTO extensionsDTO);

  /** 커스텀 확장자 등록 */
  void regCustomExtension(String extension);

  /** 커스텀 확장자 삭제 */
  void deleteCustomExtension(long id);
}
