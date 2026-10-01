# 파일 업로드 및 확장자 차단

확장자 차단 정책을 DB에서 관리하고, 실제 파일 업로드 시 서버에서 검증·저장하는 웹 애플리케이션입니다.

- **배포 사이트:** [파일 업로드 데모](https://web-file-upload-frontend-muparo2z37bbda5e.sel3.cloudtype.app)
- **백엔드:** [업로드 설정 API](https://port-0-file-upload-backend-muparo2z37bbda5e.sel3.cloudtype.app/setting)
- **기술:** React · TypeScript · Vite / Java 21 · Spring Boot · MyBatis / MariaDB
- **배포 환경:** Cloudtype — Frontend, Backend, MariaDB

## 주요 기능

- 고정 확장자 7종의 차단 설정 및 새로고침 후 유지
- 커스텀 확장자 추가·삭제, 20자·200개 제한, 대소문자 정규화와 중복 방지
- 파일 선택·드래그 앤 드롭, 파일 크기·개수 제한 설정
- 서버에서 대소문자·이중/다중 확장자 검사 및 차단 사유 안내
- UUID 기반 실제 파일 저장, 메타데이터 기록, 실패 시 파일 정리와 로그

## 로컬 실행

JDK 21, Node.js 22.12 이상인 22.x, npm, MariaDB가 필요합니다. 아래는 macOS/Linux 셸 기준입니다.

### 1. 저장소와 DB 준비

```bash
git clone https://github.com/smin89/file-upload-assignment.git
cd file-upload-assignment
```

MariaDB 관리 계정으로 아래 SQL을 실행합니다. `CHANGE_ME`는 사용할 비밀번호로 변경합니다.

```sql
CREATE DATABASE file_upload
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'file_upload_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT SELECT, INSERT, UPDATE, DELETE ON file_upload.*
  TO 'file_upload_app'@'localhost';
```

저장소 루트에서 빈 DB에 스키마를 한 번 적용합니다.

```bash
mariadb -u root -p file_upload < docs/schema.sql
```

초기 상태는 고정 확장자 모두 비활성, 커스텀 0개, 요청당 10개·파일당 10 MiB입니다.

### 2. 백엔드 실행

저장소 루트에서 실행합니다. 비밀번호는 위에서 설정한 값과 동일하게 지정합니다.

```bash
export DB_URL='jdbc:mariadb://localhost:3306/file_upload'
export DB_USERNAME='file_upload_app'
export DB_PASSWORD='CHANGE_ME'
cd backend
./gradlew bootRun
```

기본 주소는 `http://localhost:8081`이며, `GET /setting`으로 DB 조회를 확인할 수 있습니다.

### 3. 프론트엔드 실행

새 터미널에서 저장소 루트 기준으로 실행합니다.

```bash
cd frontend
npm ci
printf 'VITE_API_BASE_URL=http://localhost:8081\n' > .env
npm run dev -- --strictPort
```

기존 `.env`가 있다면 덮어쓰는 대신 API 주소를 수정합니다. [http://localhost:8080](http://localhost:8080)에서 `/upload`와 `/settings`를 이용합니다.

## 설정 및 API

DB 접속은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 지정합니다. 추가 설정은 다음과 같습니다.

| 환경 변수 | 용도·기본값 |
|---|---|
| `SERVER_PORT` | 백엔드 포트, `8081` |
| `CORS_ALLOWED_ORIGINS` | 허용 프론트엔드 출처, `http://localhost:8080` |
| `UPLOAD_DIRECTORY` | 저장 경로, 실행 위치 기준 `./data/uploads` |
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | multipart 상한, `100MB` / `110MB` |
| `VITE_API_BASE_URL` | 프론트엔드 빌드·실행 시 백엔드 주소 |

DB의 업로드 제한과 multipart 전송 상한은 별개입니다. 배포 시 CORS와 API 주소를 배포 도메인에 맞추고, 저장 경로에 쓰기 권한을 부여해야 합니다.

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | `/extensions` | 확장자 정책 조회 |
| PATCH | `/extensions/fixed` | 고정 확장자 변경 |
| POST | `/extensions/custom` | 커스텀 등록 |
| DELETE | `/extensions/custom/{id}` | 커스텀 삭제 |
| GET / PUT | `/setting` | 업로드 제한 조회·변경 |
| POST | `/files` | multipart `files` 필드로 업로드 |

응답은 `resultCode`, `message`, `data` 구조입니다. 업로드 성공은 HTTP 201이며, 차단·중복·크기 초과 등은 업무별 오류 코드와 메시지로 안내합니다.

## 테스트 및 빌드

백엔드 통합 테스트에는 실제 MariaDB가 필요합니다. **별도 테스트 DB에 스키마를 적용한 후**, 새 터미널에서 테스트 DB의 환경 변수를 지정합니다. 운영 DB에는 실행하지 않습니다.

```bash
# 저장소 루트 기준. 테스트 DB와 계정은 미리 준비합니다.
export DB_URL='jdbc:mariadb://localhost:3306/file_upload_test'
export DB_USERNAME='YOUR_TEST_USER'
export DB_PASSWORD='YOUR_TEST_PASSWORD'
cd backend
./gradlew test bootJar
```

프론트엔드는 저장소 루트의 별도 터미널에서 실행합니다.

```bash
cd frontend
npm run lint
npm run build
```

## 구현 범위와 문서

파일 목록·다운로드·삭제 API와 내용 기반 악성코드 검사는 제공하지 않습니다. 확장자 차단이 파일 내용의 안전성을 보장하지는 않습니다. 현재 데모는 인증 없는 전역 정책을 사용하며, 컨테이너 로컬 업로드 파일은 재배포 후 보존을 보장하지 않습니다.

- [설계 판단·구현 범위·보완 방향](docs/CONSIDERATIONS.md)
- [AI 입력 기록·도구 사용·회고](docs/PROMPT_LOG.md)
- [테이블 스키마](docs/schema.sql)
- [프론트엔드 상세 안내](frontend/README.md)
