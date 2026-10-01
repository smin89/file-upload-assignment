package com.smin89.fileupload;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MariaDB/MyBatis + MVC + disk. All database changes roll back after each test. */
@SpringBootTest
@Transactional
class RequirementsIntegrationTests {
  static final Path uploads;
  static { try { uploads = Files.createTempDirectory("upload-requirements-"); }
    catch (Exception e) { throw new ExceptionInInitializerError(e); } }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("app.upload.directory", uploads::toString);
  }
  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate db;
  MockMvc mvc;

  @BeforeEach void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    db.update("DELETE FROM file_extensions WHERE extension_type = 'CUSTOM'");
    db.update("UPDATE file_extensions SET enabled = FALSE WHERE extension_type = 'FIXED'");
    db.update("UPDATE upload_settings SET max_file_count=10, max_file_size=1024 WHERE id=1");
  }
  @AfterAll static void cleanup() throws Exception {
    try (var paths = Files.list(uploads)) { assertEquals(0, paths.count(), "rollback cleans uploaded bytes"); }
    Files.delete(uploads);
  }
  void add(String value, int status) throws Exception {
    mvc.perform(post("/extensions/custom").contentType("application/json")
        .content("{\"extension\":\"" + value + "\"}"))
        .andExpect(status().is(status));
  }
  void upload(String name, int bytes, int status) throws Exception {
    mvc.perform(multipart("/files").file(new MockMultipartFile("files", name, "image/png", new byte[bytes])))
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.resultCode").exists())
        .andExpect(jsonPath("$.data.files[0].storagePath").doesNotExist());
  }
  @Test void fixedListToggleAndReadback() throws Exception {
    assertEquals(Set.of("bat","cmd","com","cpl","exe","scr","js"),
        new HashSet<>(db.queryForList("SELECT extension FROM file_extensions WHERE extension_type='FIXED'", String.class)));
    long id = db.queryForObject("SELECT id FROM file_extensions WHERE extension='exe'", Long.class);
    for (boolean enabled : new boolean[]{true, false}) {
      mvc.perform(patch("/extensions/fixed").contentType("application/json")
          .content("{\"id\":"+id+",\"enabled\":"+enabled+"}"))
          .andExpect(status().isOk());
      assertEquals(enabled, db.queryForObject("SELECT enabled FROM file_extensions WHERE id=?", Boolean.class, id));
      upload("sample.exe", 1, enabled ? 400 : 201);
    }
  }
  @Test void customNormalizationDuplicatesDeletionAndLengths() throws Exception {
    add(" .SH ", 200); add("SH", 409); add("sh", 409); add("exe", 409);
    for (String value : List.of("", "   ", "x".repeat(21))) add(value,400);
    add("x".repeat(20), 200);
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM file_extensions WHERE extension='sh'", Integer.class));
    long id=db.queryForObject("SELECT id FROM file_extensions WHERE extension='sh'", Long.class);
    mvc.perform(delete("/extensions/custom/"+id)).andExpect(status().isOk());
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM file_extensions WHERE id=?", Integer.class,id));
  }
  @Test void actualTwoHundredRegistrationsAndOverflow() throws Exception {
    for (int i=0;i<200;i++) add("qa"+i,200);
    add("overflow",400);
    mvc.perform(get("/extensions")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.customExtensionCount").value(200))
        .andExpect(jsonPath("$.data.customExtensionLimit").value(200));
    assertEquals(200,db.queryForObject("SELECT COUNT(*) FROM file_extensions WHERE extension_type='CUSTOM'",Integer.class));
  }
  @Test void uploadExtensionMatrixAndManipulatedMime() throws Exception {
    db.update("UPDATE file_extensions SET enabled=TRUE WHERE extension='exe'");
    add("tar",200); add("gz",200);
    for (String name : List.of("sample.exe","sample.EXE","sample.Exe","file.exe.txt",".env.exe","archive.tar.gz",".tar.gz")) upload(name,1,400);
    for (String name : List.of("sample.txt",".env","README",".gitignore")) upload(name,1,201);
  }
  @Test void fileSizeNameTraversalAndDuplicateStorage() throws Exception {
    upload("empty.txt",0,400); upload("limit.txt",1024,201); upload("large.txt",1025,413);
    upload("a".repeat(255),1,201); upload("a".repeat(256),1,400);
    for (String name : List.of("../escape.txt","C:\\escape.txt","a/b.txt")) upload(name,1,400);
    upload("qa-same.txt",1,201); upload("qa-same.txt",1,201);
    var paths=db.queryForList("SELECT storage_path FROM files WHERE original_name='qa-same.txt'",String.class);
    assertEquals(2,paths.size()); assertNotEquals(paths.get(0),paths.get(1));
    for (String path:paths) { assertTrue(Path.of(path).startsWith(uploads)); assertEquals(1,Files.size(Path.of(path))); }
  }
}
