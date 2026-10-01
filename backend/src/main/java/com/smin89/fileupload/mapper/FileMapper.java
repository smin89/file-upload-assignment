package com.smin89.fileupload.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.smin89.fileupload.vo.FileVO;

@Mapper
public interface FileMapper {
  int insertFile(FileVO file);
}
