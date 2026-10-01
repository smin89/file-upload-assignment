package com.smin89.fileupload.dto;

import java.util.List;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

/** 변경용 명령 객체와 API별 요청·응답 타입을 함께 정의한다. */
@Data
public class ExtensionDTO {
  private long id;
  private boolean enabled;

  /** nullable 타입으로 누락된 값과 false를 구분한다. */
  public record UpdateRequest(@NotNull @Positive Long id, @NotNull Boolean enabled) {}
  public record CreateRequest(@NotBlank String extension) {}

  /** 조회 응답 필드를 명시해 Map 키 오타와 타입 혼용을 방지한다. */
  public record ListResponse(int totalExtCount, List<Fixed> fixedExtensions,
      List<Custom> customExtensions, int customExtensionCount, int customExtensionLimit) {}

  public record Fixed(Long id, String extension, boolean enabled) {
  }

  public record Custom(Long id, String extension) {
  }
}
