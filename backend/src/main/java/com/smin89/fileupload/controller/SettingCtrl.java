package com.smin89.fileupload.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smin89.fileupload.constants.ResultCode;
import com.smin89.fileupload.dto.ResultDTO;
import com.smin89.fileupload.dto.SettingDTO;
import com.smin89.fileupload.service.SettingSvc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 업로드 설정 조회·변경 요청을 서비스에 전달한다. */
@RestController
@RequestMapping("/setting")
@RequiredArgsConstructor
@Slf4j
public class SettingCtrl {
  private final SettingSvc settingSvc;

  /** DB 내부 필드를 제외하고 업로드 제한값만 반환한다. */
  @GetMapping
  public ResponseEntity<ResultDTO<SettingDTO.Response>> getSettings() {
    var settings = settingSvc.getSettings();
    var data = new SettingDTO.Response(settings.getMaxFileCount(), settings.getMaxFileSize());
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success", data));
  }

  /** 두 설정값을 함께 변경하고 적용한 값을 반환한다. 크기 단위는 byte이다. */
  @PutMapping
  public ResponseEntity<ResultDTO<SettingDTO.Response>> updateSettings(@Valid @RequestBody SettingDTO request) {
    settingSvc.updateSettings(request);
    // 커밋 이후 적용된 제한값만 기록한다.
    log.info("업로드 설정 변경 완료: maxFileCount={}, maxFileSize={}",
        request.getMaxFileCount(), request.getMaxFileSize());
    var data = new SettingDTO.Response(request.getMaxFileCount(), request.getMaxFileSize());
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success", data));
  }
}
