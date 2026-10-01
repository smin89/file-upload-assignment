package com.smin89.fileupload.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smin89.fileupload.constants.ResultCode;
import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.constants.CommonConstants;
import com.smin89.fileupload.dto.ResultDTO;
import com.smin89.fileupload.service.ExtensionSvc;
import com.smin89.fileupload.vo.ExtensionVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 확장자 정책 조회·변경 요청을 받아 서비스 결과를 공통 응답으로 반환한다. */
@RestController
@RequestMapping("/extensions")
@RequiredArgsConstructor
@Slf4j
public class ExtensionCtrl {

  private final ExtensionSvc extensionsSvc;

  /**
   * 확장자 정책 조회
   * 
   * @return 고정·커스텀 목록, 전체·커스텀 개수 및 커스텀 등록 한도
   */
  @GetMapping
  public ResponseEntity<ResultDTO<Map<String, Object>>> getExtensions() {
    String logTitle = "GetExtensions";

    List<ExtensionVO> extensions = extensionsSvc.getExtensionList();
    log.info("[{}] extensions count: {}", logTitle, extensions.size());
    log.info("[{}] extensions: {}", logTitle, extensions);

    // 고정 확장자는 차단 여부(enabled)를 함께 반환한다.
    List<ExtensionDTO.Fixed> fixedExtensions = extensions.stream()
        .filter(extension -> "FIXED".equals(extension.getExtensionType()))
        .map(extension -> new ExtensionDTO.Fixed(
            extension.getId(), extension.getExtension(), extension.isEnabled()))
        .toList();

    // 커스텀 확장자는 등록 자체가 차단을 의미하므로 별도 enabled 필드를 노출하지 않는다.
    List<ExtensionDTO.Custom> customExtensions = extensions.stream()
        .filter(extension -> "CUSTOM".equals(extension.getExtensionType()))
        .map(extension -> new ExtensionDTO.Custom(
            extension.getId(), extension.getExtension()))
        .toList();

    Map<String, Object> data = new HashMap<>();
    data.put("totalExtCount", extensions.size());
    data.put("fixedExtensions", fixedExtensions);
    data.put("customExtensions", customExtensions);
    data.put("customExtensionCount", customExtensions.size());
    data.put("customExtensionLimit", CommonConstants.MAX_EXT_COUNT);

    // HTTP 상태(200)와 응답 본문의 공통 결과 코드(CO200)를 각각 지정한다.
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success", data));
  }

  /** 같은 상태의 재요청도 성공으로 처리한다. */
  @PatchMapping("/fixed")
  public ResponseEntity<ResultDTO<Void>> updateFixedExtension(
      @jakarta.validation.Valid @RequestBody ExtensionDTO.UpdateRequest request) {
    ExtensionDTO command = new ExtensionDTO();
    command.setId(request.id());
    command.setEnabled(request.enabled());
    extensionsSvc.updateFixedExtension(command);
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success"));
  }

  @PostMapping("/custom")
  public ResponseEntity<ResultDTO<Void>> regCustomExtension(
      @jakarta.validation.Valid @RequestBody ExtensionDTO.CreateRequest request) {
    extensionsSvc.regCustomExtension(request.extension());
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success"));
  }

  @DeleteMapping("/custom/{id}")
  public ResponseEntity<ResultDTO<Void>> deleteCustomExtension(@PathVariable("id") long id) {
    extensionsSvc.deleteCustomExtension(id);
    return ResponseEntity.ok(ResultDTO.res(ResultCode.OK, "Success"));
  }
}
