package com.smin89.fileupload.extensions;

import com.smin89.fileupload.controller.ExtensionCtrl;
import com.smin89.fileupload.dto.ExtensionDTO;
import com.smin89.fileupload.exception.BusinessException;
import com.smin89.fileupload.exception.GlobalExceptionHandler;
import com.smin89.fileupload.mapper.ExtensionMapper;
import com.smin89.fileupload.service.ExtensionSvcImpl;
import com.smin89.fileupload.vo.ExtensionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExtensionWriteTests {
  ExtensionMapper mapper;
  ExtensionSvcImpl service;
  MockMvc mvc;

  @BeforeEach void setup() {
    mapper = mock(ExtensionMapper.class);
    service = new ExtensionSvcImpl(mapper);
    mvc = MockMvcBuilders.standaloneSetup(new ExtensionCtrl(service))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    when(mapper.lockCustomPolicy()).thenReturn(1);
  }

  @Test void listResponseKeepsExistingJsonFields() throws Exception {
    var fixed = row("FIXED", true);
    fixed.setExtension("exe");
    var custom = row("CUSTOM", true);
    custom.setId(2L);
    custom.setExtension("sh");
    when(mapper.getExtensionList()).thenReturn(java.util.List.of(fixed, custom));
    mvc.perform(get("/extensions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", org.hamcrest.Matchers.aMapWithSize(5)))
        .andExpect(jsonPath("$.data.totalExtCount").value(2))
        .andExpect(jsonPath("$.data.customExtensionCount").value(1))
        .andExpect(jsonPath("$.data.customExtensionLimit").value(200))
        .andExpect(jsonPath("$.data.fixedExtensions[0].enabled").value(true))
        .andExpect(jsonPath("$.data.customExtensions[0].extension").value("sh"))
        .andExpect(jsonPath("$.data.customExtensions[0].enabled").doesNotExist());
  }

  @Test void businessFailuresHaveDistinctResponseCodes() throws Exception {
    when(mapper.getExtensionByName("exe")).thenReturn(row("FIXED", false));
    mvc.perform(post("/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{\"extension\":\"EXE\"}"))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.resultCode").value("DUPLICATE_EXTENSION"));
    mvc.perform(post("/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{\"extension\":\"a b\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("INVALID_EXTENSION"));
    when(mapper.countCustomExtensions()).thenReturn(200);
    mvc.perform(post("/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{\"extension\":\"sh\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("EXTENSION_LIMIT_EXCEEDED"));
  }

  @Test void missingUpdateFieldsDoNotWrite() throws Exception {
    for (String body : new String[]{"{}", "{\"id\":1}", "{\"id\":1,\"enabled\":null}",
        "{\"enabled\":true}", "{\"id\":0,\"enabled\":false}"}) {
      mvc.perform(patch("/extensions/fixed").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("CO400"));
    }
    verifyNoInteractions(mapper);
  }

  @Test void updateAcceptsFalseAndRepeatedState() throws Exception {
    when(mapper.getExtensionForUpdate(1)).thenReturn(row("FIXED", true));
    when(mapper.updateFixedExtension(any())).thenReturn(1);
    mvc.perform(patch("/extensions/fixed").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"enabled\":false}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value("CO200"));
    when(mapper.getExtensionForUpdate(1)).thenReturn(row("FIXED", false));
    var command = new ExtensionDTO(); command.setId(1); command.setEnabled(false);
    service.updateFixedExtension(command);
    verify(mapper, times(1)).updateFixedExtension(any());
  }

  @Test void registrationNormalizesBeforeDuplicateCheckAndInsert() throws Exception {
    when(mapper.regCustomExtension("sh")).thenReturn(1);
    mvc.perform(post("/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{\"extension\":\" .SH \"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value("CO200"));
    var order = inOrder(mapper);
    order.verify(mapper).ensureCustomPolicy();
    order.verify(mapper).lockCustomPolicy();
    order.verify(mapper).getExtensionByName("sh");
    order.verify(mapper).countCustomExtensions();
    order.verify(mapper).regCustomExtension("sh");
  }

  @Test void invalidNamesAreRejected() {
    for (String input : new String[]{"", " ", ".", "..exe", "a b", "a/b", "한글", "x".repeat(21)}) {
      assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.regCustomExtension(input)).getStatus());
    }
    verifyNoInteractions(mapper);
  }

  @Test void length20AndLastAvailableSlotAreAccepted() {
    when(mapper.countCustomExtensions()).thenReturn(199);
    when(mapper.regCustomExtension("x".repeat(20))).thenReturn(1);
    assertDoesNotThrow(() -> service.regCustomExtension("x".repeat(20)));
    verify(mapper).regCustomExtension("x".repeat(20));
  }

  @Test void countLimitPreventsInsert() {
    when(mapper.countCustomExtensions()).thenReturn(200);
    assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.regCustomExtension("sh")).getStatus());
    verify(mapper, never()).regCustomExtension(anyString());
  }

  @Test void duplicateQueryAndDuplicateKeyBothReturn409() {
    when(mapper.getExtensionByName("exe")).thenReturn(row("FIXED", false));
    assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.regCustomExtension(".EXE")).getStatus());
    when(mapper.regCustomExtension("sh")).thenThrow(new DuplicateKeyException("internal SQL"));
    var error = assertThrows(BusinessException.class, () -> service.regCustomExtension("sh"));
    assertEquals(HttpStatus.CONFLICT, error.getStatus());
    assertFalse(error.getMessage().contains("internal SQL"));
  }

  @Test void deletionRejectsFixedAndMissingRows() {
    when(mapper.getExtensionForUpdate(1)).thenReturn(row("FIXED", false));
    assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.deleteCustomExtension(1)).getStatus());
    assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class, () -> service.deleteCustomExtension(2)).getStatus());
    verify(mapper, never()).deleteCustomExtension(anyLong());
  }

  @Test void customDeletionLocksBeforeDeleting() {
    when(mapper.getExtensionForUpdate(1)).thenReturn(row("CUSTOM", true));
    when(mapper.deleteCustomExtension(1)).thenReturn(1);
    service.deleteCustomExtension(1);
    var order = inOrder(mapper);
    order.verify(mapper).ensureCustomPolicy();
    order.verify(mapper).lockCustomPolicy();
    order.verify(mapper).getExtensionForUpdate(1);
    order.verify(mapper).deleteCustomExtension(1);
  }

  @Test void missingPolicyIsInitializedBeforeRegistration() {
    when(mapper.ensureCustomPolicy()).thenReturn(1);
    when(mapper.regCustomExtension("sh")).thenReturn(1);
    assertDoesNotThrow(() -> service.regCustomExtension("sh"));
    verify(mapper).regCustomExtension("sh");
    var order = inOrder(mapper);
    order.verify(mapper).ensureCustomPolicy();
    order.verify(mapper).lockCustomPolicy();
    order.verify(mapper).getExtensionByName("sh");
  }

  @Test void existingPolicyNoOpStillAllowsRegistration() {
    // MariaDB는 값이 바뀌지 않은 UPSERT의 영향 행 수로 0을 반환할 수 있다.
    when(mapper.ensureCustomPolicy()).thenReturn(0);
    when(mapper.regCustomExtension("sh")).thenReturn(1);
    assertDoesNotThrow(() -> service.regCustomExtension("sh"));
    verify(mapper).regCustomExtension("sh");
  }

  @Test void unavailablePolicyNeverProceedsToInsert() {
    when(mapper.lockCustomPolicy()).thenReturn(null);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
        assertThrows(BusinessException.class, () -> service.regCustomExtension("sh")).getStatus());
    verify(mapper, never()).regCustomExtension(anyString());
  }

  private ExtensionVO row(String type, boolean enabled) {
    var row = new ExtensionVO(); row.setId(1L); row.setExtensionType(type); row.setEnabled(enabled); return row;
  }
}
