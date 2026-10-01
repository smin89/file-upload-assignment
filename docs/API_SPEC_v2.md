# File Upload API Specification

## 1. 문서 개요

파일 확장자 차단 정책 관리와 실제 파일 업로드 기능을 위한 REST API 명세서이다.

- Backend: Spring Boot
- Database: MariaDB
- API Style: REST
- Base URL: `/` (공통 경로 접두사 없음)
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

파일 다운로드 API를 제외한 JSON API는 공통 응답 객체 `ResultDTO<T>`를 사용한다.

```java
// 성공 응답
ResultDTO.res(ResultCode.OK, "Success", data);

// 요청 오류 응답
ResultDTO.res(ResultCode.BAD_REQUEST, "잘못된 요청입니다.");

// 서버 오류 응답
ResultDTO.res(ResultCode.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
```

- `resultCode`: 애플리케이션 공통 결과 코드
- `message`: 처리 결과 메시지
- `data`: API별 응답 데이터. 제네릭 타입 `T`를 사용한다.
- 응답할 데이터가 없는 경우 `data`는 `null`로 반환한다.
- HTTP Status와 `resultCode`를 함께 사용한다. HTTP Status는 프로토콜 수준의 처리 결과를, `resultCode`는 애플리케이션의 공통 처리 결과를 나타낸다.

### 2.1 성공 응답

기본 성공 코드는 `CO200`으로 정의한다.

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": {}
}
```

커스텀 확장자 등록은 HTTP 200, 파일 업로드는 HTTP 201을 사용하며 공통 성공 결과 코드는 모두 `CO200`이다.

### 2.2 데이터가 없는 성공 응답

삭제 API도 공통 응답 형식을 유지하기 위해 `204 No Content` 대신 `200 OK`와 `data: null`을 반환한다.

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": null
}
```

### 2.3 오류 응답

오류 발생 시에도 동일한 `ResultDTO<T>` 구조를 유지한다. 오류 상세 정보가 필요하면 `data`에 객체를 반환하고, 필요하지 않으면 `null`을 반환한다.

```json
{
  "resultCode": "CO400",
  "message": "차단된 확장자가 포함된 파일입니다.",
  "data": {
    "fileName": "sample.exe.txt",
    "extension": "exe"
  }
}
```

### 2.4 결과 코드 규칙

모든 API는 `constants/ResultCode.java`에 정의된 세 가지 공통 코드를 사용한다.
도메인별 `EX`, `US`, `FI` 코드는 사용하지 않는다.

| 상수 | Result Code | 의미 |
|---|---|---|
| `ResultCode.OK` | `CO200` | 정상 처리 |
| `ResultCode.BAD_REQUEST` | `CO400` | 요청값, 정책 검증, 대상 없음, 중복 등의 요청 오류 |
| `ResultCode.INTERNAL_SERVER_ERROR` | `CO500` | 서버 내부 처리 오류 |

- HTTP Status는 오류 상황에 따라 `400`, `404`, `409`, `413`, `500` 등을 유지한다.
- 예를 들어 중복 확장자는 HTTP `409`와 `CO400`, 파일 크기 초과는 HTTP `413`과 `CO400`을 반환한다.
- 상세 원인은 `message`로 설명하고 필요한 부가 정보는 `data`에 담는다.
- 프론트엔드는 `resultCode`로 공통 결과를, HTTP Status로 상태를 구분한다. 동일 상태 내 세부 오류를 기계적으로 구분하는 별도 코드는 현재 정의하지 않는다.
- `ResultDTO` 생성만으로 HTTP Status가 지정되지 않으므로 Controller 또는 전역 예외 처리기에서 함께 지정한다.

---

# 3. 확장자 차단 정책 API

## 3.1 확장자 정책 목록 조회

고정 확장자와 커스텀 확장자 설정을 조회한다.

### Request

```http
GET /extensions
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": {
    "totalExtCount": 4,
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

- `totalExtCount`는 고정·커스텀을 포함한 전체 확장자 개수이다. 위 예시는 2개씩 총 4개이다.
- `fixedExtensions`는 `extension_type = FIXED`인 데이터를 반환한다.
- `customExtensions`는 `extension_type = CUSTOM`인 데이터를 반환한다.
- 커스텀 확장자는 등록 자체가 차단을 의미하므로 화면에서는 별도의 체크 상태를 노출하지 않는다.

---

## 3.2 고정 확장자 차단 상태 변경

```http
PATCH /extensions/fixed
Content-Type: application/json
```

```json
{"id": 1, "enabled": true}
```

- `id`: 필수, 양의 정수. 요청 본문으로 전달한다.
- `enabled`: 필수 boolean. 누락 및 null은 HTTP 400 / CO400으로 거부한다.
- 대상이 없으면 HTTP 404 / CO400, 고정 확장자가 아니면 HTTP 400 / CO400을 반환한다.
- 같은 상태의 재요청도 성공한다.
- 서비스 트랜잭션에서 대상 행 잠금 → 존재·유형 검사 → 상태 변경을 수행한다.

### Response — 200 OK

```json
{"resultCode": "CO200", "message": "Success", "data": null}
```

---

## 3.3 커스텀 확장자 추가

사용자가 입력한 확장자를 차단 목록에 추가한다.

### Request

```http
POST /extensions/custom
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

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": null
}
```

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 400 | `CO400` | 허용되지 않는 형식의 확장자 |
| 400 | `CO400` | 20자를 초과함 |
| 409 | `CO400` | 이미 등록된 확장자 |
| 409 | `CO400` | 고정 확장자와 중복됨 |
| 400 | `CO400` | 커스텀 확장자 200개 초과 |

### 서버 검증

- 정규화 후 빈 문자열을 허용하지 않는다.
- 앞뒤 공백 제거 → 선행 점 하나 제거 → 소문자 변환 후 길이와 형식을 검사한다.
- 허용 문자는 영문과 숫자이며 정규화 후 `[a-z0-9]+`만 허용한다. 내부 공백, 추가 점, 특수문자, 한글은 거부한다.
- 커스텀 등록·삭제는 서비스 트랜잭션에서 `upload_settings(id=1)` 행이 없으면 자동 생성한 뒤 잠근다. 기본값은 DB 스키마의 파일 개수 10개·파일당 10MB를 사용한다. 기존 설정값은 변경하지 않는다. 테이블 자체는 `schema.sql`로 준비해야 한다.
- 초기 행 자동 생성도 같은 트랜잭션에 포함되므로 업무 실패 시 함께 롤백된다. 동시 최초 요청은 기본키와 행 잠금으로 직렬화한다.
- 잠금 획득 후 중복과 커스텀 개수(최대 200개)를 확인하고 저장한다. 다른 애플리케이션 경로가 등록할 경우에도 동일한 잠금 규칙을 따라야 한다.
- INSERT의 중복 키 오류도 HTTP 409 / CO400으로 변환한다.
- 등록 성공은 HTTP 200 / CO200 및 `data: null`을 반환한다. 목록 갱신이 필요하면 조회 API를 호출한다.
- 정규화 후 길이는 최대 20자이다.
- `.` 자체 또는 공백이 포함된 잘못된 입력은 허용하지 않는다.
- DB의 `UNIQUE(extension)` 제약조건을 통해 동시 요청에 의한 중복도 최종 방어한다.

---

## 3.4 커스텀 확장자 삭제

등록된 커스텀 확장자를 삭제한다.

### Request

```http
DELETE /extensions/custom/{id}
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": null
}
```

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 404 | `CO400` | 해당 확장자가 존재하지 않음 |
| 400 | `CO400` | 고정 확장자 삭제 요청 |

---

# 4. 업로드 설정 API

`upload_settings`는 애플리케이션 전체에서 사용하는 전역 설정이다. 조회·변경은 `id = 1`인 행만 대상으로 하며 요청에서 ID를 받지 않는다.

설정 API 자체는 초기 행을 생성하지 않는다. 행이 없으면 조회·변경 모두 HTTP 500 / `CO500`을 반환하므로 `schema.sql`로 초기 데이터를 준비한다. 커스텀 확장자 등록·삭제 시에는 별도의 자동 생성 처리가 있다.

## 4.1 업로드 설정 조회

### Request

```http
GET /setting
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
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

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 500 | `CO500` | 설정 초기 행이 없거나 DB 조회에 실패함 |

초기 행이 없는 경우 메시지는 `업로드 설정이 초기화되지 않았습니다.`이며 `data`는 `null`이다.

---

## 4.2 업로드 설정 변경

### Request

```http
PUT /setting
Content-Type: application/json
```

```json
{
  "maxFileCount": 10,
  "maxFileSize": 10485760
}
```

### 요청 필드 및 검증

| 이름 | 타입 | 필수 | 유효값 |
|---|---|---|---|
| maxFileCount | Integer | Y | 1 이상의 정수 |
| maxFileSize | Long | Y | 1 이상의 정수, byte 단위 |

두 필드를 모두 전달한다. 필드 누락, `null`, 0, 음수 또는 변환할 수 없는 입력은 HTTP 400 / `CO400`으로 처리한다.

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": {
    "maxFileCount": 10,
    "maxFileSize": 10485760
  }
}
```

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 400 | `CO400` | 파일 개수 제한값이 유효하지 않음 |
| 400 | `CO400` | 파일 크기 제한값이 유효하지 않음 |
| 500 | `CO500` | 설정 초기 행이 없거나 DB 변경에 실패함 |

### 비고

- 설정값의 최종 유효성 검증은 서버에서 수행한다.
- 두 값을 하나의 트랜잭션으로 변경하고 성공 시 적용한 두 값을 `data`에 반환한다.
- 기존과 동일한 값으로 요청해도 HTTP 200 / `CO200`으로 응답한다.
- `id`, 생성·수정 시각 등의 DB 내부 필드는 응답에 포함하지 않는다.
- `maxFileSize`는 byte 단위로 저장한다.
- 설정 변경 이후의 업로드 요청부터 변경된 정책을 적용한다.

---

# 5. 파일 API

## 5.1 파일 업로드

하나 이상의 파일을 업로드한다.

### 저장 및 운영 설정

- 기본 저장 위치는 실행 디렉터리 기준 `./data/uploads`이며 `UPLOAD_DIRECTORY`로 변경한다. 웹 정적 리소스 경로로 지정하지 않는다.
- DB의 파일 개수·파일당 크기 제한을 매 요청마다 조회한다.
- multipart 전송 상한은 별도로 파일당 100MB, 요청 전체 110MB이다. `UPLOAD_MAX_FILE_SIZE`, `UPLOAD_MAX_REQUEST_SIZE`로 조정할 수 있다. DB 설정이 더 커도 전송 상한을 초과하면 413으로 거부된다.
- MIME 내용 판별은 구현하지 않으며 업로드 응답과 DB에는 `application/octet-stream`을 저장한다. 클라이언트 Content-Type을 신뢰하지 않는다.
- 경로 구분자, 콜론, 제어문자, 앞뒤 공백, 끝의 점을 포함한 파일명은 거부한다.
- DB 롤백 시 작성한 파일도 삭제한다. 프로세스 강제 종료, 정리 실패, 커밋 결과 불명 상황은 로그와 저장소·DB 대조를 통한 복구가 필요하다.


### Request

```http
POST /files
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
  http://localhost:8081/files
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
  "resultCode": "CO200",
  "message": "Success",
  "data": {
    "files": [
      {
        "id": 1,
        "originalName": "document.pdf",
        "mimeType": "application/octet-stream",
        "sizeBytes": 102400,
        "createdAt": "2026-09-30T02:30:00"
      },
      {
        "id": 2,
        "originalName": "image.png",
        "mimeType": "application/octet-stream",
        "sizeBytes": 204800,
        "createdAt": "2026-09-30T02:30:00"
      }
    ]
  }
}
```

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 400 | `CO400` | 업로드 파일이 없음 |
| 400 | `CO400` | 0-byte 파일 |
| 400 | `CO400` | 최대 업로드 개수 초과 |
| 413 | `CO400` | 파일 1개의 허용 크기 초과 |
| 400 | `CO400` | 유효하지 않은 파일명 |
| 400 | `CO400` | 차단된 확장자가 포함된 파일 |
| 500 | `CO500` | 파일 저장 실패 |

### 차단 확장자 오류 예시

```json
{
  "resultCode": "CO400",
  "message": "차단된 확장자가 포함된 파일입니다.",
  "data": {
    "fileName": "report.exe.txt",
    "extension": "exe"
  }
}
```

### 파일 개수 초과 예시

```json
{
  "resultCode": "CO400",
  "message": "한 번에 업로드할 수 있는 파일 개수를 초과했습니다.",
  "data": {
    "maxFileCount": 10,
    "requestedFileCount": 12
  }
}
```

### 파일 크기 초과 예시

```json
{
  "resultCode": "CO400",
  "message": "허용된 파일 크기를 초과했습니다.",
  "data": {
    "fileName": "large.zip",
    "maxFileSize": 10485760,
    "fileSize": 15728640
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
GET /files
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
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
GET /files/{id}
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
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

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 404 | `CO400` | 파일 정보가 존재하지 않음 |

---

## 5.4 파일 다운로드

### Request

```http
GET /files/{id}/download
```

### Response — 200 OK

```http
Content-Type: application/octet-stream
Content-Disposition: attachment; filename*=UTF-8''document.pdf
```

Response Body에는 실제 파일 바이너리를 반환한다.

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 404 | `CO400` | DB에 파일 정보가 없음 |
| 404 | `CO400` | DB 정보는 있으나 실제 저장 파일이 없음 |
| 500 | `CO500` | 저장 파일 읽기 실패 |

### 보안 원칙

- 클라이언트가 전달한 파일 경로를 직접 사용하지 않는다.
- `{id}`로 DB 메타데이터를 조회한 뒤 서버가 관리하는 저장 경로에서 파일을 읽는다.
- 응답 파일명은 `original_name`을 이용하되 HTTP Header에 안전하게 인코딩한다.

---

## 5.5 파일 삭제

### Request

```http
DELETE /files/{id}
```

### Response — 200 OK

```json
{
  "resultCode": "CO200",
  "message": "Success",
  "data": null
}
```

### Error

| HTTP Status | Result Code | 설명 |
|---|---|---|
| 404 | `CO400` | 삭제할 파일 정보가 없음 |
| 500 | `CO500` | 저장 파일 삭제 실패 |

---

# 6. HTTP Status Code 정책

| Status | 의미 | 사용 예 |
|---|---|---|
| 200 OK | 정상 조회/수정 | 설정 조회, 고정 확장자 변경 |
| 201 Created | 리소스 생성 성공 | 파일 업로드 |
| 400 Bad Request | 요청값/정책 검증 실패 | 차단 확장자, 잘못된 입력, 개수 초과 |
| 404 Not Found | 리소스 없음 | 파일/확장자 없음 |
| 409 Conflict | 현재 데이터와 충돌 | 확장자 중복 |
| 413 Payload Too Large | 파일 크기 제한 초과 | 업로드 파일 크기 초과 |
| 500 Internal Server Error | 서버 내부 오류 | 파일 저장/삭제 실패 |

---

# 7. Result Code 정리

공통 결과 코드는 `ResultCode` 상수를 사용하며 코드 문자열을 각 API에 직접 작성하지 않는다.

| 상수 | Result Code | HTTP Status | 설명 |
|---|---|---|---|
| `OK` | `CO200` | 200, 201 | 조회·수정·삭제·생성 성공 |
| `BAD_REQUEST` | `CO400` | 400, 404, 409, 413 | 입력값 및 정책 검증 실패, 대상 없음, 중복, 크기 초과 |
| `INTERNAL_SERVER_ERROR` | `CO500` | 500 | DB, 파일 저장·읽기·삭제 등 서버 오류 |

응답은 `resultCode`, `message`, `data`로 통일하며 데이터가 없으면 `data: null`을 반환한다.
파일 다운로드 성공은 바이너리로 반환하고, 다운로드를 시작하기 전에 발생한 오류는 공통 JSON 형식으로 반환한다.

# 8. DB 테이블과 API 관계

| Table | 사용 API | 목적 |
|---|---|---|
| `files` | `/files/**` | 업로드 파일 메타데이터 저장 |
| `upload_settings` | `/setting` | 전역 업로드 개수/크기 제한 설정 |
| `file_extensions` | `/extensions/**` | 고정/커스텀 확장자 차단 정책 저장 |

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
| GET | `/extensions` | 확장자 정책 조회 |
| PATCH | `/extensions/fixed` | 고정 확장자 차단 여부 변경 |
| POST | `/extensions/custom` | 커스텀 확장자 추가 |
| DELETE | `/extensions/custom/{id}` | 커스텀 확장자 삭제 |
| GET | `/setting` | 업로드 설정 조회 |
| PUT | `/setting` | 업로드 설정 변경 |
| POST | `/files` | 파일 업로드 |
| GET | `/files` | 파일 목록 조회 |
| GET | `/files/{id}` | 파일 상세 조회 |
| GET | `/files/{id}/download` | 파일 다운로드 |
| DELETE | `/files/{id}` | 파일 삭제 |
