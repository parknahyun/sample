# 커머스 상품 관리

커머스 운영자가 판매할 상품(상품명·설명·가격·옵션별 재고·카테고리·이미지)을 등록·조회·수정·삭제하고, 판매 상태를
관리하며, 검색·필터·정렬하는 관리자 웹 기능이다. 고객 화면이 상품을 읽을 수 있도록 인증 없는 읽기 전용 조회 API도
함께 제공한다.

- 백엔드: Java 21, Spring Boot 3.5, PostgreSQL 16 (`backend/`)
- 프런트엔드: React 18, TypeScript, Vite (`frontend/`)
- 명세·계획·작업 목록: [`specs/001-product-management/`](specs/001-product-management/)
  - [spec.md](specs/001-product-management/spec.md): 기능 명세
  - [plan.md](specs/001-product-management/plan.md), [research.md](specs/001-product-management/research.md), [data-model.md](specs/001-product-management/data-model.md)
  - [contracts/product-api.openapi.yaml](specs/001-product-management/contracts/product-api.openapi.yaml): API 계약
  - [quickstart.md](specs/001-product-management/quickstart.md): 실행과 검증 시나리오
- 프로젝트 원칙: [`.specify/memory/constitution.md`](.specify/memory/constitution.md) (모든 문서는 한글로 작성한다)

## 사전 준비

- JDK 21, Node.js 22 이상, Docker
- 저장소 루트에서 `.env.example`을 `.env`로 복사하고 값을 채운다. `.env`는 커밋하지 않는다.
- 프런트엔드 개발 서버용 자격 증명은 `frontend/.env`에도 같은 값으로 둔다(`frontend/.env.example` 참고).

## 로컬 실행

### 방식 A: DB만 Docker, 앱은 로컬 실행 (일상 개발)

```bash
# 1. 데이터베이스 시작
docker compose up -d postgres

# 2. 백엔드 실행 (개발 프로필). .env의 값을 환경 변수로 읽어 들인 뒤 실행한다.
set -a; source .env; set +a                     # Git Bash
cd backend && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# 3. 프런트엔드 실행 (다른 터미널)
cd frontend && npm install && npm run dev       # http://localhost:5173
```

- 백엔드는 기본적으로 8080 포트를 쓴다. 이미 사용 중이면 `SERVER_PORT=18080`으로 바꾸고, 프런트엔드는
  `API_PROXY_TARGET=http://localhost:18080 npm run dev`로 백엔드 주소를 알려 준다.
- 개발 프로필은 `.env`의 `DEV_OPERATOR_USERNAME`/`DEV_OPERATOR_PASSWORD` 계정(HTTP Basic, `PRODUCT_MANAGER`
  역할)으로 인증한다. 개발 프로필이 아니면 인증 가능한 계정이 없어 모든 운영자 API가 401이다(운영 인증은 별도 기능).

### 방식 B: 전체를 Docker로 실행 (`full` 프로필, 온보딩·통합 확인)

```bash
docker compose --profile full up --build        # 프런트엔드 http://localhost:5173, 백엔드 http://localhost:8080
docker compose --profile full down              # 종료 (데이터까지 삭제하려면 down -v)
```

- 포트가 겹치면 `.env`의 `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT`를 바꾼다.
- **개발 전용이다.** 프런트엔드 번들에 개발용 운영자 계정이 포함되므로 운영 배포나 외부 공개에 쓰지 않는다.
- 업로드한 이미지는 이름 있는 볼륨 `imagedata`에 보존된다.

## 테스트와 품질 게이트

```bash
cd backend && ./gradlew check                   # 포맷(Spotless)·정적 분석(Checkstyle)·단위·통합·계약 테스트
cd frontend && npm run lint && npm test         # ESLint·Prettier, Vitest
cd frontend && npx playwright install chromium  # 처음 한 번
cd frontend && npm run test:e2e                 # E2E: 백엔드·DB·프런트엔드 개발 서버가 실행 중이어야 한다
```

- 통합·계약 테스트는 Testcontainers로 실제 PostgreSQL을 쓰므로 Docker가 필요하다. Docker 없이 일부만 확인하려면
  `./gradlew test -PwithoutDocker`로 통합 테스트(태그 `integration`)를 제외할 수 있다. 병합 게이트에서는 이 옵션을 쓰지 않는다.
- 포맷은 `./gradlew spotlessApply`와 `npm run format`으로 맞춘다.
- `QuickstartScenariosTest`는 quickstart의 검증 시나리오가 모두 자동 테스트에 대응하는지 확인한다.

## 구조

```text
backend/    Spring Boot API (상품·이미지·카테고리·고객 조회·변경 이력)
frontend/   운영자 웹 화면 (목록·검색·등록·수정·상세·삭제)
specs/      기능 명세, 계획, 계약, 작업 목록
docker-compose.yml  postgres(기본), backend·frontend(`full` 프로필)
```

API는 `/api/v1` 아래에 있다. 운영자 API는 `PRODUCT_MANAGER` 역할이 필요하고, `GET /api/v1/storefront/**`와
`GET /api/v1/images/**`만 인증 없이 읽을 수 있다. 오류 응답은 Problem Details(RFC 9457) 형식이다.
