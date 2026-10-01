package com.smin89.fileupload.files;

import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import com.smin89.fileupload.controller.FileCtrl;
import com.smin89.fileupload.dto.FileDTO;
import com.smin89.fileupload.exception.*;
import com.smin89.fileupload.mapper.FileMapper;
import com.smin89.fileupload.service.*;
import com.smin89.fileupload.storage.LocalFileStorage;
import com.smin89.fileupload.vo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileUploadTests {
  @TempDir Path directory;
  FileMapper mapper;
  SettingSvc settings;
  ExtensionSvc extensions;
  FileSvcImpl service;
  SettingVO limits;
  TransactionTemplate tx;
  List<FileVO> rows;

  @BeforeEach void setup() {
    mapper = mock(FileMapper.class);
    settings = mock(SettingSvc.class);
    extensions = mock(ExtensionSvc.class);
    limits = new SettingVO(); limits.setMaxFileCount(10); limits.setMaxFileSize(100);
    when(settings.getSettings()).thenReturn(limits);
    when(extensions.getExtensionList()).thenReturn(List.of(policy("exe", "FIXED", true), policy("sh", "CUSTOM", false)));
    rows = new ArrayList<>();
    AtomicLong ids = new AtomicLong();
    when(mapper.insertFile(any())).thenAnswer(call -> {
      FileVO row = call.getArgument(0); row.setId(ids.incrementAndGet()); rows.add(row); return 1;
    });
    service = new FileSvcImpl(mapper, settings, extensions, new LocalFileStorage(directory.toString()));
    // 실제 파일 I/O와 트랜잭션 동기화 콜백을 검증한다. DB SQL은 이 테스트의 범위가 아니다.
    tx = new TransactionTemplate(new AbstractPlatformTransactionManager() {
      protected Object doGetTransaction() { return new Object(); }
      protected void doBegin(Object t, TransactionDefinition d) {}
      protected void doCommit(DefaultTransactionStatus s) {}
      protected void doRollback(DefaultTransactionStatus s) {}
    });
  }

  @Test void savesBytesHashAndUniqueNames() throws Exception {
    var result = upload(List.of(file("a.txt", "abc"), file("a.txt", "abc")));
    assertEquals(2, result.files().size());
    assertNotEquals(rows.get(0).getStoredName(), rows.get(1).getStoredName());
    for (var row : rows) {
      assertEquals("abc", Files.readString(Path.of(row.getStoragePath())));
      assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", row.getSha256());
      assertEquals(3, row.getSizeBytes());
      assertEquals("application/octet-stream", row.getMimeType());
    }
  }

  @Test void rejectsAllExtensionCandidatesIgnoringCase() throws Exception {
    for (String name : List.of("a.EXE", "a.exe.txt", ".env.exe", "a.sh", "a.sh.txt")) {
      var error = assertThrows(BusinessException.class, () -> upload(List.of(file(name, "x"))));
      assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
      assertTrue(error.getData() instanceof Map);
    }
    verifyNoInteractions(mapper);
    assertEmpty();
  }

  @Test void allowsDotfilesNoExtensionAndDisabledFixedPolicy() {
    when(extensions.getExtensionList()).thenReturn(List.of(policy("exe", "FIXED", false)));
    assertEquals(4, upload(List.of(file(".env", "x"), file("README", "x"), file("a.exe", "x"), file("archive.tar.gz", "x"))).files().size());
  }

  @Test void validatesEntireBatchBeforeWriting() throws Exception {
    assertThrows(BusinessException.class, () -> upload(List.of(file("ok.txt", "x"), file("a.exe", "x"))));
    verifyNoInteractions(mapper);
    assertEmpty();
  }

  @Test void rejectsMissingEmptyOversizeAndTooManyFiles() {
    assertThrows(BusinessException.class, () -> upload(null));
    assertThrows(BusinessException.class, () -> upload(List.of()));
    assertThrows(BusinessException.class, () -> upload(List.of(file("a", ""))));
    limits.setMaxFileSize(2);
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
        assertThrows(BusinessException.class, () -> upload(List.of(file("a", "abc")))).getStatus());
    limits.setMaxFileCount(1);
    assertThrows(BusinessException.class, () -> upload(List.of(file("a", "x"), file("b", "x"))));
    verifyNoInteractions(mapper);
  }

  @Test void acceptsExactSizeLimit() { limits.setMaxFileSize(3); assertEquals(1, upload(List.of(file("a", "abc"))).files().size()); }

  @Test void rejectsUnsafeNames() {
    for (String name : List.of("../a", "C:\\a", "a/b", "a\nb", " a.txt", "a.", "a".repeat(256))) {
      assertThrows(BusinessException.class, () -> upload(List.of(file(name, "x"))));
    }
    verifyNoInteractions(mapper);
  }

  @Test void removesAllFilesWhenSecondDbInsertFails() throws Exception {
    doReturn(1).doThrow(new IllegalStateException("DB unavailable")).when(mapper).insertFile(any());
    assertThrows(IllegalStateException.class, () -> upload(List.of(file("a", "x"), file("b", "x"))));
    assertEmpty();
  }

  @Test void removesPartialFileWhenInputFails() throws Exception {
    var failing = new MockMultipartFile("files", "b", "text/plain", new byte[]{1}) {
      @Override public InputStream getInputStream() throws IOException { throw new IOException("read failure"); }
    };
    assertThrows(BusinessException.class, () -> upload(List.of(file("a", "x"), failing)));
    assertEmpty();
  }

  @Test void checksActualStreamSizeAndCleansUp() throws Exception {
    limits.setMaxFileSize(2);
    var lying = new MockMultipartFile("files", "a", "text/plain", "abcd".getBytes()) {
      @Override public long getSize() { return 1; }
    };
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
        assertThrows(BusinessException.class, () -> upload(List.of(lying))).getStatus());
    assertEmpty();
  }

  @Test void apiUses201AndHidesInternalMetadata() throws Exception {
    FileSvc transactionalService = files -> upload(files);
    var mvc = MockMvcBuilders.standaloneSetup(new FileCtrl(transactionalService))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    mvc.perform(multipart("/files").file(file("a.txt", "abc")))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.resultCode").value("CO200"))
        .andExpect(jsonPath("$.data.files[0].originalName").value("a.txt"))
        .andExpect(jsonPath("$.data.files[0].storedName").doesNotExist())
        .andExpect(jsonPath("$.data.files[0].storagePath").doesNotExist())
        .andExpect(jsonPath("$.data.files[0].sha256").doesNotExist());
    mvc.perform(multipart("/files")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("CO400"));
    mvc.perform(multipart("/files").file(file("a.exe.txt", "abc")))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.extension").value("exe"));
  }

  private FileDTO.UploadResult upload(List<MultipartFile> files) { return tx.execute(status -> service.uploadFiles(files)); }
  private MockMultipartFile file(String name, String text) { return new MockMultipartFile("files", name, "text/plain", text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
  private ExtensionVO policy(String name, String type, boolean enabled) {
    var row = new ExtensionVO(); row.setExtension(name); row.setExtensionType(type); row.setEnabled(enabled); return row;
  }
  private void assertEmpty() throws IOException { try (var files = Files.list(directory)) { assertEquals(0, files.count()); } }
}
