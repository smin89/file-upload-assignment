package com.smin89.fileupload.settings;

import com.smin89.fileupload.controller.SettingCtrl;
import com.smin89.fileupload.exception.GlobalExceptionHandler;
import com.smin89.fileupload.mapper.SettingMapper;
import com.smin89.fileupload.service.SettingSvcImpl;
import com.smin89.fileupload.vo.SettingVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SettingCtrlTests {
  private SettingMapper mapper;
  private MockMvc mvc;
  @BeforeEach void setup() {
    mapper = mock(SettingMapper.class);
    mvc = MockMvcBuilders.standaloneSetup(new SettingCtrl(new SettingSvcImpl(mapper)))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }
  @Test void getReturnsOnlyPublicSettings() throws Exception {
    var row = new SettingVO(); row.setMaxFileCount(10); row.setMaxFileSize(10485760);
    when(mapper.getSettings()).thenReturn(row);
    mvc.perform(get("/setting")).andExpect(status().isOk())
        .andExpect(jsonPath("$.resultCode").value("CO200"))
        .andExpect(jsonPath("$.data", aMapWithSize(2)))
        .andExpect(jsonPath("$.data.maxFileCount").value(10))
        .andExpect(jsonPath("$.data.maxFileSize").value(10485760));
  }
  @Test void updateReturnsAppliedSettings() throws Exception {
    when(mapper.updateSettings(any())).thenReturn(1);
    mvc.perform(put("/setting").contentType(MediaType.APPLICATION_JSON)
        .content("{\"maxFileCount\":5,\"maxFileSize\":2048}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.maxFileCount").value(5))
        .andExpect(jsonPath("$.data.maxFileSize").value(2048));
    verify(mapper).updateSettings(argThat(value -> value.getMaxFileCount() == 5 && value.getMaxFileSize() == 2048));
  }
  @Test void invalidOrMissingSettingsDoNotWrite() throws Exception {
    for (String body : new String[]{"{}", "{\"maxFileCount\":1}", "{\"maxFileSize\":1}",
        "{\"maxFileCount\":0,\"maxFileSize\":1}", "{\"maxFileCount\":1,\"maxFileSize\":-1}"}) {
      mvc.perform(put("/setting").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("CO400"));
    }
    verifyNoInteractions(mapper);
  }
  @Test void missingSettingsDoNotReturnSuccess() throws Exception {
    mvc.perform(get("/setting")).andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.resultCode").value("CO500"));
    mvc.perform(put("/setting").contentType(MediaType.APPLICATION_JSON)
        .content("{\"maxFileCount\":5,\"maxFileSize\":2048}"))
        .andExpect(status().isInternalServerError());
  }
  @Test void unchangedSettingsAreSuccessful() throws Exception {
    var row = new SettingVO(); row.setMaxFileCount(5); row.setMaxFileSize(2048);
    when(mapper.getSettings()).thenReturn(row);
    mvc.perform(put("/setting").contentType(MediaType.APPLICATION_JSON)
        .content("{\"maxFileCount\":5,\"maxFileSize\":2048}"))
        .andExpect(status().isOk());
  }
}
