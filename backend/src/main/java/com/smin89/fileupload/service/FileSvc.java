package com.smin89.fileupload.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.smin89.fileupload.dto.FileDTO;

public interface FileSvc {
  FileDTO.UploadResult uploadFiles(List<MultipartFile> files);
}
