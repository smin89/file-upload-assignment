package com.smin89.fileupload.dto;

import org.apache.ibatis.type.Alias;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

@Data
@Alias("extensionsDTO")
public class ExtensionDTO {
  private long id;
  private String extension;
  private boolean enabled;

  /** nullable 타입으로 누락된 값과 false를 구분한다. */
  public record UpdateRequest(@NotNull @Positive Long id, @NotNull Boolean enabled) {}
  public record CreateRequest(@NotBlank String extension) {}

  public record Fixed(Long id, String extension, boolean enabled) {
  }

  public record Custom(Long id, String extension) {
  }
}
