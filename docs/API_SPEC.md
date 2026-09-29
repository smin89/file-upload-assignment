# File Upload API Specification

## 1. 문서 개요

파일 확장자 차단 정책 관리와 실제 파일 업로드 기능을 위한 REST API 명세서이다.

- Backend: Spring Boot
- Database: MariaDB
- API Style: REST
- Base URL: `/api`
- Response Content-Type: `application/json`
- File Upload Content-Type: `multipart/form-data`

### 주요 정책

- 고정 확장자: `bat`, `cmd`, `com`, `cpl`, `exe`, `scr`, `js`
- 고정 확장자는 최초 생성 시 모두 차단 해제(`enabled=false`) 상태이다.
- 커스텀 확장자는 최대 20자, 최대 200개까지 등록할 수 있다.
- 확장자는 소문자로 정규화하여 저장/비교한다.
- 고정/커스텀 확장자는 중복 등록할 수 없다.
- 파일 업로드 시 서버에서 최종적으로 확장자 차단 정책을 검사한다.
- `file.exe.txt`처럼 확장자가 여러 개인 경우 파일명에 포함된 확장자 후보를 모두 검사한다.
- 확장자가 없는 파일과 `.env` 같은 dotfile은 허용한다.
- 0-byte 파일은 업로드를 거부한다.
- 저장 파일명은 UUID 기반으로 생성하고 원본 파일명을 그대로 저장 경로에 사용하지 않는다.
- MIME Type / Magic Number 기반 파일 내용 검증은 이번 구현 범위에서 제외한다.

---

## 2. 공통 응답 규격

### 2.1 성공 응답

API 특성에 따라 필요한 데이터를 `data`에 반환한다.

```json
{
  "success": true,
  "data": {}
}
```

### 2.2 오류 응답

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류에 대한 설명"
  }
}
```

파일과 관련된 오류처럼 추가 정보가 필요한 경우 `details`를 포함할 수 있다.

```json
{
  "success": false,
  "error": {
    "code": "BLOCKED_EXTENSION",
    "message": "차단된 확장자가 포함된 파일입니다.",
    "details": {
      "fileName": "sample.exe.txt",
      "extension": "exe"
    }
  }
}
```

---

# 3. 확장자 차단 정책 API

## 3.1 확장자 정책 목록 조회

고정 확장자와 커스텀 확장자 설정을 조회한다.

### Request

```http
GET /api/extensions
```

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "fixedExtensions": [
      {
        "id": 1,
        "extension": "bat",
        "enabled": false
      },
      {
        "id": 2,
        "extension": "cmd",
        "enabled": true
      }
    ],
    "customExtensions": [
      {
        "id": 8,
        "extension": "sh"
      },
      {
        "id": 9,
        "extension": "jar"
      }
    ],
    "customExtensionCount": 2,
    "customExtensionLimit": 200
  }
}
```

### 비고

- `fixedExtensions`는 `extension_type = FIXED`인 데이터를 반환한다.
- `customExtensions`는 `extension_type = CUSTOM`인 데이터를 반환한다.
- 커스텀 확장자는 등록 자체가 차단을 의미하므로 화면에서는 별도의 체크 상태를 노출하지 않는다.

---

## 3.2 고정 확장자 차단 상태 변경

고정 확장자의 checked/unchecked 상태를 변경한다.

### Request

```http
PATCH /api/extensions/fixed/{id}
Content-Type: application/json
```

```json
{
  "enabled": true
}
```

### Path Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | Long | Y | `file_extensions.id` |

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| enabled | Boolean | Y | `true`: 차단, `false`: 차단 해제 |

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "id": 1,
    "extension": "bat",
    "enabled": true
  }
}
```

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_REQUEST | enabled 값이 없거나 잘못된 요청 |
| 404 | EXTENSION_NOT_FOUND | 해당 확장자가 존재하지 않음 |
| 400 | NOT_FIXED_EXTENSION | 대상이 고정 확장자가 아님 |

---

## 3.3 커스텀 확장자 추가

사용자가 입력한 확장자를 차단 목록에 추가한다.

### Request

```http
POST /api/extensions/custom
Content-Type: application/json
```

```json
{
  "extension": "sh"
}
```

### 입력 정규화

서버에서 다음 순서로 정규화한다.

1. 앞뒤 공백 제거
2. 앞의 `.` 제거
3. 소문자 변환
4. 허용 문자 및 길이 검증
5. 고정/커스텀 전체 확장자와 중복 여부 확인

예:

```text
" .SH " → "sh"
```

### Response — 201 Created

```json
{
  "success": true,
  "data": {
    "id": 8,
    "extension": "sh"
  }
}
```

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_EXTENSION | 허용되지 않는 형식의 확장자 |
| 400 | EXTENSION_TOO_LONG | 20자를 초과함 |
| 409 | DUPLICATE_EXTENSION | 이미 등록된 확장자 |
| 409 | FIXED_EXTENSION_CONFLICT | 고정 확장자와 중복됨 |
| 400 | CUSTOM_EXTENSION_LIMIT_EXCEEDED | 커스텀 확장자 200개 초과 |

### 서버 검증

- 빈 문자열을 허용하지 않는다.
- 정규화 후 길이는 최대 20자이다.
- `.` 자체 또는 공백이 포함된 잘못된 입력은 허용하지 않는다.
- DB의 `UNIQUE(extension)` 제약조건을 통해 동시 요청에 의한 중복도 최종 방어한다.

---

## 3.4 커스텀 확장자 삭제

등록된 커스텀 확장자를 삭제한다.

### Request

```http
DELETE /api/extensions/custom/{id}
```

### Response — 204 No Content

응답 Body 없음.

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 404 | EXTENSION_NOT_FOUND | 해당 확장자가 존재하지 않음 |
| 400 | NOT_CUSTOM_EXTENSION | 고정 확장자 삭제 요청 |

---

# 4. 업로드 설정 API

`upload_settings`는 애플리케이션 전체에서 사용하는 전역 설정이며 DB에 하나의 설정 행만 존재하는 것을 전제로 한다.

## 4.1 업로드 설정 조회

### Request

```http
GET /api/upload-settings
```

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "maxFileCount": 10,
    "maxFileSize": 10485760
  }
}
```

### 필드 설명

| 이름 | 타입 | 단위 | 설명 |
|---|---|---|---|
| maxFileCount | Integer | 개 | 한 요청에서 업로드할 수 있는 최대 파일 수 |
| maxFileSize | Long | byte | 파일 1개당 최대 크기 |

---

## 4.2 업로드 설정 변경

### Request

```http
PUT /api/upload-settings
Content-Type: application/json
```

```json
{
  "maxFileCount": 10,
  "maxFileSize": 10485760
}
```

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "maxFileCount": 10,
    "maxFileSize": 10485760
  }
}
```

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_MAX_FILE_COUNT | 파일 개수 제한값이 유효하지 않음 |
| 400 | INVALID_MAX_FILE_SIZE | 파일 크기 제한값이 유효하지 않음 |

### 비고

- 설정값의 최종 유효성 검증은 서버에서 수행한다.
- `maxFileSize`는 byte 단위로 저장한다.
- 설정 변경 이후의 업로드 요청부터 변경된 정책을 적용한다.

---

# 5. 파일 API

## 5.1 파일 업로드

하나 이상의 파일을 업로드한다.

### Request

```http
POST /api/files
Content-Type: multipart/form-data
```

### Form Data

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| files | MultipartFile[] | Y | 업로드 파일 목록 |

### 예시

```bash
curl -X POST \
  -F "files=@document.pdf" \
  -F "files=@image.png" \
  http://localhost:8080/api/files
```

### 서버 검증 순서

1. 요청에 파일이 존재하는지 확인
2. 업로드 파일 개수가 `maxFileCount` 이하인지 확인
3. 각 파일이 0-byte인지 확인
4. 각 파일 크기가 `maxFileSize` 이하인지 확인
5. 파일명 안전성 및 확장자 추출
6. 파일명에 포함된 확장자 후보를 모두 추출하고 소문자로 정규화
7. 활성화된 고정 확장자 및 커스텀 확장자 차단 정책과 비교
8. UUID 기반 저장 파일명 생성
9. 파일 저장
10. `files` 테이블에 메타데이터 저장

### 다중 확장자 검사 예시

```text
파일명: report.exe.txt
검사 대상: exe, txt

exe가 차단되어 있으면 업로드 거부
```

```text
파일명: archive.tar.gz
검사 대상: tar, gz
```

### Response — 201 Created

```json
{
  "success": true,
  "data": {
    "files": [
      {
        "id": 1,
        "originalName": "document.pdf",
        "mimeType": "application/pdf",
        "sizeBytes": 102400,
        "createdAt": "2026-09-30T02:30:00"
      },
      {
        "id": 2,
        "originalName": "image.png",
        "mimeType": "image/png",
        "sizeBytes": 204800,
        "createdAt": "2026-09-30T02:30:00"
      }
    ]
  }
}
```

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 400 | FILE_REQUIRED | 업로드 파일이 없음 |
| 400 | EMPTY_FILE | 0-byte 파일 |
| 400 | FILE_COUNT_EXCEEDED | 최대 업로드 개수 초과 |
| 413 | FILE_SIZE_EXCEEDED | 파일 1개의 허용 크기 초과 |
| 400 | INVALID_FILE_NAME | 유효하지 않은 파일명 |
| 400 | BLOCKED_EXTENSION | 차단된 확장자가 포함된 파일 |
| 500 | FILE_STORAGE_ERROR | 파일 저장 실패 |

### 차단 확장자 오류 예시

```json
{
  "success": false,
  "error": {
    "code": "BLOCKED_EXTENSION",
    "message": "차단된 확장자가 포함된 파일입니다.",
    "details": {
      "fileName": "report.exe.txt",
      "extension": "exe"
    }
  }
}
```

### 파일 개수 초과 예시

```json
{
  "success": false,
  "error": {
    "code": "FILE_COUNT_EXCEEDED",
    "message": "한 번에 업로드할 수 있는 파일 개수를 초과했습니다.",
    "details": {
      "maxFileCount": 10,
      "requestedFileCount": 12
    }
  }
}
```

### 파일 크기 초과 예시

```json
{
  "success": false,
  "error": {
    "code": "FILE_SIZE_EXCEEDED",
    "message": "허용된 파일 크기를 초과했습니다.",
    "details": {
      "fileName": "large.zip",
      "maxFileSize": 10485760,
      "fileSize": 15728640
    }
  }
}
```

### 업로드 처리 원칙

다중 파일 요청에서는 **요청 단위 All-or-Nothing** 방식을 사용한다.

파일을 실제 저장하기 전에 모든 파일에 대한 기본 검증과 확장자 정책 검사를 먼저 수행하며, 하나라도 검증에 실패하면 해당 요청 전체를 거부한다. 이를 통해 사용자가 어떤 파일만 저장되었는지 판단하기 어려운 부분 성공 상태를 피한다.

파일 저장/DB 기록 도중 시스템 오류가 발생한 경우 이미 저장된 파일은 정리하는 보상 처리를 수행한다.

---

## 5.2 업로드 파일 목록 조회

### Request

```http
GET /api/files
```

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "files": [
      {
        "id": 1,
        "originalName": "document.pdf",
        "mimeType": "application/pdf",
        "sizeBytes": 102400,
        "createdAt": "2026-09-30T02:30:00"
      }
    ]
  }
}
```

### 비고

- `stored_name`, `storage_path`는 서버 내부 정보이므로 일반 조회 응답에는 노출하지 않는다.
- 파일 수가 커질 경우 pagination 도입을 고려한다.

---

## 5.3 파일 상세 조회

### Request

```http
GET /api/files/{id}
```

### Response — 200 OK

```json
{
  "success": true,
  "data": {
    "id": 1,
    "originalName": "document.pdf",
    "mimeType": "application/pdf",
    "sizeBytes": 102400,
    "sha256": "7b3d...",
    "createdAt": "2026-09-30T02:30:00"
  }
}
```

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 404 | FILE_NOT_FOUND | 파일 정보가 존재하지 않음 |

---

## 5.4 파일 다운로드

### Request

```http
GET /api/files/{id}/download
```

### Response — 200 OK

```http
Content-Type: application/octet-stream
Content-Disposition: attachment; filename*=UTF-8''document.pdf
```

Response Body에는 실제 파일 바이너리를 반환한다.

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 404 | FILE_NOT_FOUND | DB에 파일 정보가 없음 |
| 404 | STORED_FILE_NOT_FOUND | DB 정보는 있으나 실제 저장 파일이 없음 |
| 500 | FILE_READ_ERROR | 저장 파일 읽기 실패 |

### 보안 원칙

- 클라이언트가 전달한 파일 경로를 직접 사용하지 않는다.
- `{id}`로 DB 메타데이터를 조회한 뒤 서버가 관리하는 저장 경로에서 파일을 읽는다.
- 응답 파일명은 `original_name`을 이용하되 HTTP Header에 안전하게 인코딩한다.

---

## 5.5 파일 삭제

### Request

```http
DELETE /api/files/{id}
```

### Response — 204 No Content

응답 Body 없음.

### Error

| HTTP Status | Code | 설명 |
|---|---|---|
| 404 | FILE_NOT_FOUND | 삭제할 파일 정보가 없음 |
| 500 | FILE_DELETE_ERROR | 저장 파일 삭제 실패 |

---

# 6. HTTP Status Code 정책

| Status | 의미 | 사용 예 |
|---|---|---|
| 200 OK | 정상 조회/수정 | 설정 조회, 고정 확장자 변경 |
| 201 Created | 리소스 생성 성공 | 커스텀 확장자 추가, 파일 업로드 |
| 204 No Content | 삭제 성공 | 확장자/파일 삭제 |
| 400 Bad Request | 요청값/정책 검증 실패 | 차단 확장자, 잘못된 입력, 개수 초과 |
| 404 Not Found | 리소스 없음 | 파일/확장자 없음 |
| 409 Conflict | 현재 데이터와 충돌 | 확장자 중복 |
| 413 Payload Too Large | 파일 크기 제한 초과 | 업로드 파일 크기 초과 |
| 500 Internal Server Error | 서버 내부 오류 | 파일 저장/삭제 실패 |

---

# 7. Error Code 정리

| Code | HTTP Status | 설명 |
|---|---:|---|
| INVALID_REQUEST | 400 | 잘못된 요청 |
| INVALID_EXTENSION | 400 | 유효하지 않은 확장자 |
| EXTENSION_TOO_LONG | 400 | 확장자 20자 초과 |
| DUPLICATE_EXTENSION | 409 | 커스텀 확장자 중복 |
| FIXED_EXTENSION_CONFLICT | 409 | 고정 확장자와 중복 |
| CUSTOM_EXTENSION_LIMIT_EXCEEDED | 400 | 커스텀 확장자 200개 초과 |
| EXTENSION_NOT_FOUND | 404 | 확장자 없음 |
| NOT_FIXED_EXTENSION | 400 | 고정 확장자가 아님 |
| NOT_CUSTOM_EXTENSION | 400 | 커스텀 확장자가 아님 |
| INVALID_MAX_FILE_COUNT | 400 | 잘못된 파일 개수 설정 |
| INVALID_MAX_FILE_SIZE | 400 | 잘못된 파일 크기 설정 |
| FILE_REQUIRED | 400 | 업로드 파일 없음 |
| EMPTY_FILE | 400 | 0-byte 파일 |
| FILE_COUNT_EXCEEDED | 400 | 업로드 가능 파일 개수 초과 |
| FILE_SIZE_EXCEEDED | 413 | 파일 크기 제한 초과 |
| INVALID_FILE_NAME | 400 | 유효하지 않은 파일명 |
| BLOCKED_EXTENSION | 400 | 차단된 확장자 포함 |
| FILE_NOT_FOUND | 404 | 파일 메타데이터 없음 |
| STORED_FILE_NOT_FOUND | 404 | 실제 저장 파일 없음 |
| FILE_STORAGE_ERROR | 500 | 파일 저장 실패 |
| FILE_READ_ERROR | 500 | 파일 읽기 실패 |
| FILE_DELETE_ERROR | 500 | 파일 삭제 실패 |

---

# 8. DB 테이블과 API 관계

| Table | 사용 API | 목적 |
|---|---|---|
| `files` | `/api/files/**` | 업로드 파일 메타데이터 저장 |
| `upload_settings` | `/api/upload-settings` | 전역 업로드 개수/크기 제한 설정 |
| `file_extensions` | `/api/extensions/**` | 고정/커스텀 확장자 차단 정책 저장 |

`files`의 주요 컬럼은 `id`, `original_name`, `stored_name`, `storage_path`, `mime_type`, `size_bytes`, `sha256`, `created_at`을 기준으로 한다.

`file_extensions`는 `extension_type`으로 `FIXED`와 `CUSTOM`을 구분한다. 고정 확장자는 `enabled` 값으로 차단 여부를 관리하며, 커스텀 확장자는 등록된 항목을 차단 대상으로 취급한다.

---

# 9. 구현 시 추가 고려사항

## 9.1 서버 사이드 검증

프론트엔드의 확장자/파일 크기 검증은 사용자 편의를 위한 1차 검증으로만 사용한다. 실제 업로드 허용 여부는 반드시 서버가 DB의 현재 정책을 조회하여 결정한다.

## 9.2 파일명과 저장 경로

원본 파일명을 실제 저장 파일명으로 사용하지 않는다. UUID 등의 서버 생성 식별자를 사용하여 저장하고, 원본 파일명은 DB 메타데이터로만 관리한다. 이를 통해 파일명 충돌 및 경로 조작 위험을 줄인다.

## 9.3 확장자 판별

단순히 마지막 `.` 뒤 문자열만 검사하지 않고 파일명에 포함된 확장자 후보를 모두 검사한다. 비교 시 대소문자 우회를 방지하기 위해 소문자로 정규화한다.

## 9.4 MIME Type / 파일 시그니처

클라이언트가 전달하는 MIME Type은 신뢰할 수 없으며 MIME 스푸핑 가능성이 있다. 다만 이번 과제에서는 요구사항의 핵심인 확장자 차단 정책에 집중하기 위해 파일 시그니처(Magic Number) 검증은 구현 범위에서 제외한다. 향후 보안 요구 수준이 높아질 경우 Apache Tika 또는 파일 시그니처 기반 검증을 추가할 수 있다.

## 9.5 SHA-256

업로드 완료 시 파일의 SHA-256 해시를 계산하여 `files.sha256`에 저장한다. 이번 범위에서는 중복 파일 차단 용도로 사용하지 않고 파일 무결성 확인 및 향후 확장 가능성을 위한 메타데이터로 사용한다.

---

# 10. API 요약

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/extensions` | 확장자 정책 조회 |
| PATCH | `/api/extensions/fixed/{id}` | 고정 확장자 차단 여부 변경 |
| POST | `/api/extensions/custom` | 커스텀 확장자 추가 |
| DELETE | `/api/extensions/custom/{id}` | 커스텀 확장자 삭제 |
| GET | `/api/upload-settings` | 업로드 설정 조회 |
| PUT | `/api/upload-settings` | 업로드 설정 변경 |
| POST | `/api/files` | 파일 업로드 |
| GET | `/api/files` | 파일 목록 조회 |
| GET | `/api/files/{id}` | 파일 상세 조회 |
| GET | `/api/files/{id}/download` | 파일 다운로드 |
| DELETE | `/api/files/{id}` | 파일 삭제 |
