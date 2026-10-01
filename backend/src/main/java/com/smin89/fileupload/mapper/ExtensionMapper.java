package com.smin89.fileupload.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.vo.ExtensionVO;

/** MyBatis가 동일한 namespace와 메서드 id의 XML SQL을 실행하는 DB 접근 인터페이스. */
@Mapper
public interface ExtensionMapper {
  List<ExtensionVO> getExtensionList();

  ExtensionVO getExtension(@Param("id") long id);

  int updateFixedExtension(ExtensionDTO extensionsDTO);

  ExtensionVO getExtensionByName(@Param("extension") String extension);

  int regCustomExtension(@Param("extension") String extension);

  ExtensionVO getExtensionForUpdate(@Param("id") long id);

  /** 누락된 singleton만 기본값으로 생성하고 기존 설정은 유지한다. */
  int ensureCustomPolicy();

  Integer lockCustomPolicy();

  int countCustomExtensions();

  int deleteCustomExtension(@Param("id") long id);
}
