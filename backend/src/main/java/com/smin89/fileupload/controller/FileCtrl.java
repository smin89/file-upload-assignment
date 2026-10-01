package com.smin89.fileupload.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smin89.fileupload.constants.ResultCode;
import com.smin89.fileupload.dto.FileDTO;
import com.smin89.fileupload.dto.ResultDTO;
import com.smin89.fileupload.service.FileSvc;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileCtrl {
  private final FileSvc fileSvc;

  /** multipart의 files 필드로 여러 파일을 받는다. 누락 오류는 서비스에서 공통 처리한다. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ResultDTO<FileDTO.UploadResult>> uploadFiles(
      @RequestParam(value = "files", required = false) List<MultipartFile> files) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ResultDTO.res(ResultCode.OK, "Success", fileSvc.uploadFiles(files)));
  }
}
