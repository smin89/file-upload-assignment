package com.smin89.fileupload.exception;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTests {
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new TestController())
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test
  void databaseFailureReturnsCommonServerErrorWithoutInternalDetails() throws Exception {
    mvc.perform(get("/test/db"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.resultCode").value("CO500"))
        .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(content().string(not(containsString("internal-db-host"))));
  }

  @Test
  void invalidParameterReturnsCommonRequestError() throws Exception {
    mvc.perform(get("/test/input").param("id", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resultCode").value("CO400"));
  }

  @Test
  void unsupportedMethodPreservesStatusAndAllowHeader() throws Exception {
    mvc.perform(post("/test/input"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", containsString("GET")))
        .andExpect(jsonPath("$.resultCode").value("CO400"));
  }

  @Test
  void explicitNotFoundPreservesStatusWithoutExposingReason() throws Exception {
    mvc.perform(get("/test/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.resultCode").value("CO400"))
        .andExpect(content().string(not(containsString("internal detail"))));
  }

  @Test
  void businessErrorPreservesPublicMessageAndDetails() throws Exception {
    mvc.perform(get("/test/blocked"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resultCode").value("CO400"))
        .andExpect(jsonPath("$.message").value("차단된 확장자가 포함된 파일입니다."))
        .andExpect(jsonPath("$.data.fileName").value("sample.exe"))
        .andExpect(jsonPath("$.data.extension").value("exe"));
  }

  @Test
  void businessErrorsPreserveHttpStatusAndUseCommonCodes() throws Exception {
    for (int status : new int[] {404, 409, 413, 500}) {
      mvc.perform(get("/test/business").param("status", String.valueOf(status)))
          .andExpect(status().is(status))
          .andExpect(jsonPath("$.resultCode").value(status == 500 ? "CO500" : "CO400"))
          .andExpect(jsonPath("$.message").value("공개 가능한 업무 오류"))
          .andExpect(jsonPath("$.data").value(nullValue()));
    }
  }

  @Test
  void businessErrorRejectsSuccessStatus() {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> new BusinessException(HttpStatus.OK, "잘못된 상태"));
  }

  @RestController
  static class TestController {
    @GetMapping("/test/blocked")
    public String blocked() {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "차단된 확장자가 포함된 파일입니다.",
          Map.of("fileName", "sample.exe", "extension", "exe"));
    }

    @GetMapping("/test/business")
    public String business(@RequestParam("status") int status) {
      throw new BusinessException(HttpStatus.valueOf(status), "공개 가능한 업무 오류");
    }

    @GetMapping("/test/db")
    public String databaseFailure() {
      throw new DataAccessResourceFailureException("internal-db-host");
    }

    @GetMapping("/test/input")
    public int input(@RequestParam("id") int id) { return id; }

    @GetMapping("/test/missing")
    public String missing() {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "internal detail");
    }
  }
}
