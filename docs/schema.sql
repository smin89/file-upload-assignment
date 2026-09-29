-- =========================================================
-- File Upload Assignment - MariaDB Schema
-- =========================================================

-- =========================================================
-- 1. 업로드 파일 정보
-- =========================================================
CREATE TABLE files (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '파일 ID',

    original_name VARCHAR(255) NOT NULL COMMENT '업로드한 원본 파일명',
    stored_name VARCHAR(255) NOT NULL COMMENT '서버에 저장된 파일명',
    storage_path VARCHAR(1000) NOT NULL COMMENT '파일 저장 경로',

    mime_type VARCHAR(100) NOT NULL COMMENT '서버에서 확인한 MIME 타입',
    size_bytes BIGINT NOT NULL COMMENT '파일 크기(Byte)',
    sha256 CHAR(64) NOT NULL COMMENT '파일 SHA-256 해시값',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',

    PRIMARY KEY (id),
    UNIQUE KEY uk_files_stored_name (stored_name)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='업로드 파일 정보';


-- =========================================================
-- 2. 파일 업로드 전역 설정
--
-- 시스템 전체에서 하나의 설정만 사용한다.
-- 애플리케이션에서는 id = 1인 행만 조회/수정한다.
-- =========================================================
CREATE TABLE upload_settings (
    id TINYINT UNSIGNED NOT NULL COMMENT '전역 설정 ID (항상 1)',

    max_file_count INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '1회 최대 업로드 파일 개수',
    max_file_size BIGINT UNSIGNED NOT NULL DEFAULT 10485760 COMMENT '파일당 최대 크기(Byte)',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    PRIMARY KEY (id),
    CONSTRAINT chk_upload_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_upload_settings_max_file_count CHECK (max_file_count > 0),
    CONSTRAINT chk_upload_settings_max_file_size CHECK (max_file_size > 0)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='파일 업로드 전역 설정';


-- =========================================================
-- 3. 제한 확장자 설정
-- =========================================================
CREATE TABLE file_extensions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '확장자 ID',

    extension VARCHAR(20) NOT NULL COMMENT '확장자 (예: exe, bat)',
    extension_type VARCHAR(20) NOT NULL COMMENT '확장자 유형 (FIXED, CUSTOM)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '제한 활성화 여부',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    PRIMARY KEY (id),
    UNIQUE KEY uk_file_extensions_extension (extension),
    CONSTRAINT chk_file_extensions_type
        CHECK (extension_type IN ('FIXED', 'CUSTOM'))
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='파일 제한 확장자 설정';


-- =========================================================
-- 초기 데이터
-- =========================================================

-- 전역 업로드 설정 (10개 / 파일당 10MB)
INSERT INTO upload_settings (
    id,
    max_file_count,
    max_file_size
) VALUES (
    1,
    10,
    10485760
);


-- 고정 확장자 초기 데이터
INSERT INTO file_extensions (
    extension,
    extension_type,
    enabled
) VALUES
    ('bat', 'FIXED', FALSE),
    ('cmd', 'FIXED', FALSE),
    ('com', 'FIXED', FALSE),
    ('cpl', 'FIXED', FALSE),
    ('exe', 'FIXED', FALSE),
    ('scr', 'FIXED', FALSE),
    ('js',  'FIXED', FALSE);
