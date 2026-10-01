# 파일 업로드 프런트엔드

파일 업로드와 업로드 정책 관리를 위한 React 애플리케이션입니다. Spring Boot 백엔드와 HTTP API로 통신합니다.

## 주요 기능

- **파일 업로드 (`/upload`)**: 드래그 앤 드롭 또는 파일 선택, 중복 파일 제외, 파일 크기·개수·차단 확장자 검증, 업로드 결과 표시
- **설정 (`/settings`)**: 고정 차단 확장자 선택, 사용자 지정 차단 확장자 추가·삭제, 최대 파일 크기와 업로드 개수 설정
- 루트 경로(`/`)는 `/upload`로 이동합니다.

## 기술 구성

React 19, TypeScript 6, Vite 8, React Router 7, Axios를 사용합니다. 코드 검사는 ESLint, 코드 포맷은 Prettier로 관리합니다.

## 로컬 실행

### 1. 준비 및 의존성 설치

Node.js `20.19.x 이상인 20.x` 또는 `22.12.0 이상`과 npm이 필요합니다. 백엔드와 백엔드가 사용하는 DB도 별도로 실행해야 합니다.

저장소 루트에서 다음 명령을 실행합니다.

```bash
cd frontend
npm ci
```

이후 명령은 모두 `frontend` 디렉터리에서 실행합니다.

### 2. API 주소 설정

`frontend/.env` 파일을 만들고 백엔드 주소를 입력합니다.

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

현재 백엔드 기본 포트는 `8081`이며 API 경로 앞에 `/api`는 붙이지 않습니다. `.env`는 Git에서 제외됩니다. `VITE_` 환경 변수는 브라우저에 노출되므로 비밀키나 비밀번호를 넣지 않습니다. 값을 변경했다면 개발 서버를 재시작하고, 배포 빌드는 다시 생성합니다.

### 3. 개발 서버 실행

```bash
npm run dev
```

프런트엔드는 **http://localhost:8080**, 백엔드는 **http://localhost:8081**을 사용합니다. 백엔드의 `WebConfig.java`도 `http://localhost:8080`을 CORS 허용 출처로 설정하고 있어 `npm run dev`로 실행하면 됩니다. 백엔드 설정을 변경한 뒤에는 백엔드를 재시작합니다.

8080 포트가 이미 사용 중이면 Vite가 다른 포트를 선택할 수 있습니다. CORS 설정과 주소를 일치시키려면 기존 프로세스를 종료하거나 다음 명령으로 지정 포트에서만 실행합니다.

```bash
npm run dev -- --strictPort
```

접속 주소는 터미널 출력에서 확인합니다. 다른 호스트나 포트를 사용하면 백엔드 CORS 허용 출처도 함께 변경해야 합니다. 프런트엔드 개발 서버에는 API 프록시가 설정되어 있지 않습니다.

## 명령어

| 명령어 | 용도 |
| --- | --- |
| `npm run dev` | 개발 서버 실행 |
| `npm run lint` | ESLint 검사 |
| `npm run build` | TypeScript 검사 후 `dist/`에 배포 파일 생성 |
| `npm run preview` | 빌드 결과를 로컬에서 확인 (`build` 실행 후 사용) |
| `npx prettier --check src` | 소스 포맷 검사 |
| `npx prettier --write src` | 소스 포맷 적용 |

`preview`는 로컬 확인용입니다. 해당 주소에서 API를 호출하려면 백엔드 CORS 허용 출처도 맞아야 합니다. 실제 배포 서버는 `/upload`, `/settings`로 직접 접속해도 `index.html`을 반환하도록 SPA 경로 처리를 설정해야 합니다.

## 폴더 구조

```text
frontend/
├── public/                  # 정적 파일
├── src/
│   ├── api/                 # Axios 인스턴스와 API 호출 함수
│   ├── components/
│   │   ├── common/          # 공통 레이아웃
│   │   ├── file/            # 파일 선택·목록 컴포넌트
│   │   └── setting/         # 업로드 정책 설정 컴포넌트
│   ├── pages/               # FileUploadPage, SettingsPage
│   ├── styles/
│   │   ├── common.css       # 공통 레이아웃·스타일
│   │   └── pages/           # 페이지별 CSS
│   ├── types/               # API 응답·요청 및 도메인 타입
│   ├── utils/               # 파일 검증·표시·API 오류 처리
│   ├── App.tsx              # 라우팅
│   ├── main.tsx             # 앱 진입점
│   └── index.css            # 전역 기본 스타일
├── eslint.config.js
├── tsconfig.app.json
└── vite.config.ts
```

## 코드 작성 기준

- `@/`는 `src/`를 가리킵니다. 내부 모듈은 `@/api/settingApi`와 같은 경로로 import합니다. 별칭은 TypeScript와 Vite 양쪽에 설정되어 있습니다.
- import는 외부 라이브러리 → 내부 모듈 → 내부 타입 → CSS 순으로 배치합니다. 타입 전용 import에는 `import type`을 사용합니다.
- 전역 기본 스타일은 `index.css`, 공통 스타일은 `styles/common.css`, 페이지 전용 스타일은 `styles/pages/`에서 관리합니다. 페이지 CSS는 해당 페이지에서 import합니다.
- 파일 크기는 API와 상태에서 **byte** 단위로 유지합니다. 설정 화면에서만 **MB(1 MB = 1,024 × 1,024 byte)**로 변환합니다.
- 프런트엔드 파일 검증은 사용자 안내용이며, 최종 허용 여부는 서버에서 다시 검증합니다. 다른 화면에서 정책이 변경되면 서버의 최신 정책에 따라 업로드가 거절될 수 있습니다.

## 백엔드 연동

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| `GET` | `/setting` | 업로드 제한 조회 |
| `PUT` | `/setting` | 업로드 제한 변경 |
| `GET` | `/extensions` | 차단 확장자 조회 |
| `PATCH` | `/extensions/fixed` | 고정 확장자 차단 여부 변경 |
| `POST` | `/extensions/custom` | 사용자 지정 확장자 추가 |
| `DELETE` | `/extensions/custom/{id}` | 사용자 지정 확장자 삭제 |
| `POST` | `/files` | `files` 필드에 파일을 담은 multipart 업로드 |

Axios의 현재 요청 제한 시간은 10초입니다. 업로드 허용 크기는 화면에서 설정한 정책 외에도 백엔드의 multipart 제한을 적용받습니다.
