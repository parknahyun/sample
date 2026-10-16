# 구현 계획 (Implementation Plan): 커머스 상품 관리

**브랜치**: `001-product-management` | **날짜**: 2026-10-07 | **명세**: [spec.md](./spec.md)

**입력**: `/specs/001-product-management/spec.md`의 기능 명세

## 요약 (Summary)

커머스 운영자가 상품(상품명·설명·가격·옵션별 재고·카테고리·이미지)을 등록·조회·수정·삭제하고, 판매 상태를
관리하며, 검색·필터·정렬할 수 있는 관리자 웹 기능을 만든다. 삭제는 논리 삭제, 동시 수정은 낙관적 락으로
충돌 감지, 상품 코드는 자동 생성(직접 입력 허용), 모든 변경은 변경 이력으로 기록한다.

기술적 접근: Spring Boot REST API(백엔드)와 React SPA(프런트엔드)로 분리한다. 데이터는 PostgreSQL에
저장하고, 이미지 파일은 저장소 인터페이스 뒤의 로컬 파일 시스템에 둔다. 상세 결정과 근거는
[research.md](./research.md)에 있다.

## 기술 맥락 (Technical Context)

**언어/버전**: 백엔드 Java 21, 프런트엔드 TypeScript 5.x

**주요 의존성**: Spring Boot 3.5(Web, Data JPA, Validation, Security), Flyway, Logback JSON 인코더,
OpenAPI 문서(springdoc); React 18, Vite, TanStack Query, React Hook Form

**저장소**: PostgreSQL 16 (이미지 파일은 로컬 파일 시스템, `ImageStorage` 인터페이스 뒤에 격리)

**테스트**: JUnit 5 + Spring Boot Test + Testcontainers(PostgreSQL), Vitest + React Testing Library,
Playwright(핵심 사용자 여정 E2E)

**대상 플랫폼**: Linux 서버(백엔드), 데스크톱 웹 브라우저(운영자 화면; 모바일 전용 화면은 범위 밖)

**로컬 개발 환경**: Docker Compose. 기본은 DB(PostgreSQL)만 컨테이너로 띄우고 백엔드·프런트엔드는 로컬에서
실행한다(핫 리로드·디버깅 용이). `full` 프로필로 DB + 백엔드 + 프런트엔드를 모두 컨테이너로 실행할 수도 있다.

**프로젝트 유형**: web-application (backend + frontend)

**성능 목표**: 상품 1만 개 기준 검색 응답 p95 1초 미만(SC-002의 5초 이내 목표에 여유 확보),
상품 수정이 고객 조회에 1분 이내 반영(SC-003, 캐시 없이 DB 직접 조회로 즉시 반영)

**제약 조건**: 이미지 JPG·PNG, 상품당 10장, 장당 5MB 이하; 가격은 0 초과 원화 정수; 재고는 0 이상 정수;
모든 관리 API는 권한 있는 운영자 전용

**규모/범위**: 상품 1만 개, 동시 운영자 수 명 단위, 화면 3~4개(목록·등록/수정·상세)

## 헌법 점검 (Constitution Check)

*게이트: Phase 0 조사 전에 통과해야 한다. Phase 1 설계 후 다시 점검한다.*

| 원칙 | 점검 결과 | 계획 반영 |
|------|-----------|-----------|
| I. 코드 품질 | 통과 | 백엔드는 Spotless(google-java-format)와 Checkstyle, 프런트엔드는 ESLint·Prettier를 병합 게이트로 설정. 공개 API는 OpenAPI로 문서화 |
| II. 테스트 우선 | 통과 | 작업 목록(tasks)에서 모든 구현 작업 앞에 실패하는 테스트 작성 작업을 배치. 버그 수정은 회귀 테스트 포함 |
| III. 단순성 | 통과 (정당화 3건) | 이미지 저장소는 로컬 파일만 구현하고 인터페이스만 둠(교체 필요성은 테스트 격리로 정당화). 캐시·검색엔진·메시지 큐는 도입하지 않음. 백엔드/프런트엔드 분리와 Docker 풀스택 프로필은 아래 복잡성 추적에 기록 |
| IV. 관측 가능성 | 통과 | 구조화(JSON) 로그, 요청 ID, 오류 응답에 조치 가능한 메시지. 로그에 인증 정보·개인정보 금지 |
| V. 버전 관리 | 통과 | API 경로에 `/api/v1` 사용. 호환성 파괴 변경은 MAJOR와 마이그레이션 안내 |
| VI. 한글 문서화 | 통과 | 계획·조사·데이터 모델·계약 설명·퀵스타트·코드 주석·커밋·PR 설명 모두 한글. 식별자·경로·명령어는 원문 유지 |
| 기술·보안 제약 | 통과 | 비밀 정보는 환경 변수, 입력은 API 경계에서 Bean Validation으로 검증, 신규 의존성은 research.md에서 필요성·유지보수·라이선스 검토 |

**Phase 1 설계 후 재점검**: 통과. 데이터 모델·계약이 위 결정과 일치하며 새로운 위반이 없다.

## 프로젝트 구조 (Project Structure)

### 문서 (이 기능)

```text
specs/001-product-management/
├── plan.md              # 이 파일 (/speckit-plan 명령 출력)
├── research.md          # Phase 0 출력 (/speckit-plan 명령)
├── data-model.md        # Phase 1 출력 (/speckit-plan 명령)
├── quickstart.md        # Phase 1 출력 (/speckit-plan 명령)
├── contracts/
│   └── product-api.openapi.yaml   # Phase 1 출력 (/speckit-plan 명령)
└── tasks.md             # Phase 2 출력 (/speckit-tasks 명령 - /speckit-plan이 생성하지 않음)
```

### 소스 코드 (저장소 루트)

```text
docker-compose.yml           # postgres(기본), backend·frontend(`full` 프로필)
.env.example                 # 환경 변수 키 목록(값 없음). 실제 `.env`는 커밋하지 않음
backend/
├── Dockerfile               # `full` 프로필용 멀티 스테이지 빌드
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/com/example/commerce/
│   │   │   ├── product/
│   │   │   │   ├── domain/          # Product, ProductOptionGroup, ProductVariant, ProductImage
│   │   │   │   ├── repository/      # Spring Data JPA 리포지토리, 검색 사양(Specification)
│   │   │   │   ├── service/         # 스토리별 서비스(등록·수정·삭제·조회), 재고·상태 규칙, 변경 이력 기록
│   │   │   │   └── api/             # 스토리별 REST 컨트롤러(등록·수정·삭제·조회), 요청/응답 DTO, 예외 매핑
│   │   │   ├── storefront/          # 고객 노출용 읽기 전용 조회 API(FR-018)
│   │   │   ├── category/            # Category 엔터티, 조회 API
│   │   │   ├── image/               # ImageStorage 인터페이스, 로컬 파일 구현, 업로드 검증
│   │   │   ├── audit/               # ProductChangeLog 엔터티와 기록기
│   │   │   └── config/              # 보안, 로깅, 파일 저장 설정
│   │   └── resources/
│   │       └── db/migration/        # Flyway 마이그레이션
│   └── test/
│       └── java/com/example/commerce/
│           ├── contract/            # OpenAPI 계약 테스트
│           ├── integration/         # Testcontainers 기반 통합 테스트
│           └── unit/                # 도메인 규칙 단위 테스트
frontend/
├── Dockerfile               # `full` 프로필용 빌드 + 정적 서빙
├── vite.config.ts           # 개발 서버의 `/api` 프록시(백엔드로 전달)
├── package.json
├── src/
│   ├── pages/                       # 상품 목록, 상품 등록/수정, 상품 상세
│   ├── components/                  # 옵션 편집기, 이미지 업로더, 삭제 확인 대화상자 등
│   ├── services/                    # API 클라이언트(OpenAPI 계약 기반)
│   └── hooks/
└── tests/
    ├── unit/
    └── e2e/                         # Playwright
```

**구조 결정**: 사용자가 선택한 Java + Spring Boot + React 스택에 맞춰 `backend/`와 `frontend/` 두 프로젝트로
나눈다. 백엔드는 기능 단위(product, category, image, audit) 패키지로 구성한다.

## 복잡성 추적 (Complexity Tracking)

| 위반 사항 | 필요한 이유 | 더 단순한 대안을 기각한 이유 |
|-----------|------------|-------------------------------------|
| 프로젝트 2개(backend + frontend) | 선택한 스택(Spring Boot API + React SPA)이 본질적으로 분리 구조이며, 이후 고객용 화면·주문 기능이 같은 API를 재사용해야 함 | 단일 프로젝트(서버 렌더링)는 사용자가 선택한 스택과 맞지 않음 |
| Docker Compose `full` 프로필(앱 Dockerfile 2개) | 신규 팀원 온보딩과 통합 확인 시 JDK·Node 설치 없이 `docker compose --profile full up`으로 전체를 실행해야 함 | DB만 컨테이너로 두면 앱 실행 환경을 각자 맞춰야 함. 단, 기본 개발 흐름은 DB만 컨테이너로 사용하고 풀스택은 선택 사항으로 한정해 유지 비용을 낮춤 |
| `ImageStorage` 인터페이스 | 테스트에서 임시 디렉터리로 격리하고 이후 객체 저장소로 교체할 수 있어야 함 | 파일 시스템 직접 호출은 테스트 격리가 어렵고 교체 시 서비스 계층 전반을 수정해야 함 |
