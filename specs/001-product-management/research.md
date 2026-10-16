# 조사 (Research): 커머스 상품 관리

**날짜**: 2026-10-07 | **계획**: [plan.md](./plan.md)

기술 맥락의 모든 `NEEDS CLARIFICATION`은 사용자가 스택(Java + Spring Boot + React)을 선택하면서 해소되었다.
이 문서는 그 위에서 내린 설계 결정과 근거를 기록한다.

## 1. 동시 수정 충돌 감지 (FR-007)

- **결정**: JPA `@Version` 낙관적 락을 사용한다. 응답에 `version`을 포함하고, 수정 요청이 `version`을
  함께 보내며 불일치하면 `409 Conflict`와 안내 메시지를 반환한다. 프런트엔드는 409를 받으면 최신 내용을
  다시 불러오라고 안내한다.
- **근거**: 명세 결정(충돌 시 나중 저장 거부)과 정확히 일치하고 별도 잠금 인프라가 필요 없다.
- **검토한 대안**: 마지막 저장 우선(자동 덮어쓰기 금지 요구와 충돌), 편집 잠금(명세에서 기각, 잠금 해제
  관리가 복잡).

## 2. 논리 삭제 (FR-012)

- **결정**: `products.deleted_at` 컬럼으로 삭제를 표시한다. 조회는 기본적으로 `deleted_at IS NULL`만
  대상으로 하고(Hibernate `@SQLRestriction`), 주문 등 다른 기능이 과거 상품 정보를 읽을 수 있도록
  삭제된 행도 보존한다.
- **근거**: 모든 상품 삭제를 논리 삭제로 한다는 명확화 결정을 따르며, 과거 주문 정보 보존(SC-006)을 보장한다.
- **검토한 대안**: 물리 삭제 + 주문 시점 스냅샷(주문 기능이 아직 없어 범위 초과), 이력 유무별 분기 삭제(규칙
  복잡).

## 3. 상품 코드 (FR-005)

- **결정**: 코드 컬럼에 고유 인덱스를 두고(삭제된 상품 포함), 입력이 비어 있으면 DB 시퀀스로 `PRD-000001`
  형식을 생성한다. 직접 입력 시 형식(영문 대문자·숫자·하이픈, 3~30자)을 검증하고 중복이면 `409`로
  거부한다. 수정 API는 코드를 변경할 수 없다.
- **근거**: 시퀀스는 동시 등록에서도 충돌이 없다. 삭제된 상품의 코드까지 포함해 고유하게 해야 과거 주문과
  혼동되지 않는다.
- **검토한 대안**: UUID(운영자가 읽기 어려움), 애플리케이션에서 최대값+1 계산(동시성 취약).

## 4. 판매 상태와 재고 (FR-008, FR-009, FR-015, FR-016)

- **결정**: 상태는 `ON_SALE`(판매중), `SOLD_OUT`(품절), `HIDDEN`(숨김) 세 값을 저장한다.
  - 운영자가 직접 지정할 수 있는 값은 `ON_SALE`과 `HIDDEN`이다. `SOLD_OUT`은 재고에서 도출되는 상태다.
  - 상품이 `HIDDEN`이 아니면 서비스 계층이 저장 시마다 재고 합계(옵션 상품은 모든 옵션 조합의 합,
    옵션 없는 상품은 상품 재고)를 계산해 0이면 `SOLD_OUT`, 1 이상이면 `ON_SALE`로 맞춘다.
  - `HIDDEN`은 재고 변화와 무관하게 유지한다.
- **근거**: 명세의 "품절 자동 표시 및 자동 복귀, 숨김 유지" 결정을 단일 규칙 함수로 구현할 수 있어
  테스트하기 쉽다. 운영자가 재고가 있는데 수동으로 품절 처리하는 요구는 명세에 없으므로 도입하지 않는다
  (단순성).
- **검토한 대안**: 상태를 저장하지 않고 항상 계산(숨김 처리와 섞여 쿼리가 복잡해지고 필터링이 느려짐).

## 5. 옵션과 옵션 조합 재고 (FR-001, FR-015)

- **결정**: 옵션 그룹(예: 색상, 사이즈)과 옵션 값을 정의하고, 옵션 값의 조합마다 `ProductVariant`
  (조합 키와 재고)를 둔다. 같은 조합의 중복은 `(product_id, combination_key)` 고유 제약으로 막는다.
  가격은 상품 단위로만 보유한다. 옵션이 없는 상품은 상품 재고를 직접 사용한다.
- **근거**: 가격은 상품당 하나이고 재고만 조합별로 관리한다는 명세와 일치하며 모델이 가장 작다.
- **검토한 대안**: 조합별 가격(명세에서 제외), 단순 목록 옵션(조합별 재고 표현 불가).

## 6. 검색·필터·정렬·페이지네이션 (FR-010, FR-011, SC-002)

- **결정**: Spring Data JPA `Specification`으로 상품명 부분 일치, 카테고리, 판매 상태 필터를 조합한다.
  정렬은 등록일·가격, 페이지는 `page`/`size`(기본 20, 최대 100)로 제공한다. 상품명 부분 일치에는
  PostgreSQL `pg_trgm` GIN 인덱스를 둔다.
- **근거**: 1만 건 규모에서 인덱스된 `ILIKE`로 목표를 충분히 만족하며 별도 검색 엔진이 필요 없다.
- **검토한 대안**: Elasticsearch(규모 대비 과함, 단순성 원칙 위배).

## 7. 이미지 저장 (FR-002)

- **결정**: 업로드 시 확장자뿐 아니라 파일 시그니처(매직 넘버)로 JPG·PNG 여부를 검증하고, 5MB 초과와 상품당
  10장 초과를 거부한다. 저장 파일명은 서버가 생성한 UUID를 사용한다(원본 파일명 미사용). 메타데이터
  (대표 여부, 표시 순서, 저장 키)는 DB에 둔다. 파일은 `ImageStorage` 인터페이스의 로컬 파일 시스템
  구현에 저장한다.
- **이미지 개수**: 이미지는 선택이며(0~10장) 등록과 이미지 업로드는 별도 단계다. 등록 직후 업로드가 실패해도 상품은 이미지 없이 남고, 운영자는 수정 화면에서 다시 올릴 수 있다. 대표 이미지를 삭제하면 표시 순서가 가장 앞선 남은 이미지가 대표가 된다. 이미지 업로드·삭제·대표 변경도 변경 이력(`UPDATED`)에 요약을 남긴다(FR-013).
- **이미지 제공**: 저장된 파일은 `GET /api/v1/images/{storageKey}`로 인증 없이 읽을 수 있다(고객 화면에서도 표시해야 함). `storageKey`는 추측하기 어려운 서버 생성 UUID이다. 삭제된 상품의 이미지는 404이다. 숨김 상품의 이미지는 목록·상세 어디에도 노출되지 않지만, 키를 아는 경우에는 읽을 수 있다(운영자 화면의 `<img>`는 인증 헤더를 보내지 못하므로 숨김 상품의 썸네일을 보이려면 이 방식이 필요하다). 응답에는 저장된 `content_type`을 쓰고 `X-Content-Type-Options: nosniff`를 붙인다.
- **근거**: 시그니처 검증과 서버 생성 파일명은 업로드를 통한 경로 조작과 위장 파일을 막는 보안 제약
  (경계에서 외부 입력 검증)을 충족한다.
- **검토한 대안**: 객체 저장소(S3 등)는 이번 범위에서 도입하지 않고 인터페이스로 교체 가능성만 남긴다.

## 8. 변경 이력 (FR-013)

- **결정**: `product_change_logs` 테이블에 변경자, 시각, 작업 종류(생성·수정·삭제), 변경 요약(JSON)을
  기록한다. 상품 변경과 같은 트랜잭션에서 저장해 둘 중 하나만 반영되는 일이 없게 한다.
- **근거**: "누가, 언제, 무엇을" 요구를 만족하며 감사 추적의 일관성을 보장한다.
- **검토한 대안**: 별도 이벤트 발행 후 비동기 기록(유실 가능, 현재 규모에 과함).

## 9. 인증과 권한 (FR-014, FR-017)

- **결정**: Spring Security의 메서드/경로 단위 권한으로 모든 상품 관리 API를 `PRODUCT_MANAGER` 역할에만
  허용한다. 단일 판매자이므로 소유권 검사는 두지 않는다. 사용자 계정 발급·로그인 화면은 이 기능의 범위 밖이며,
  개발·테스트 프로필에서는 인메모리 사용자를 사용한다.
- **근거**: 명세 가정("인증된 운영자")과 단일 판매자 전제를 그대로 따른다.
- **검토한 대안**: 이 기능에서 로그인·계정 관리까지 구현(범위 초과).
- **개발·테스트 인증**: dev·테스트 프로필은 HTTP Basic으로 인메모리 `PRODUCT_MANAGER`를 인증한다(계정은
  `.env`의 환경 변수). 프런트엔드 개발 서버(Vite)는 `/api`를 백엔드로 프록시해 CORS 설정이 필요 없다.
  프런트엔드 API 클라이언트와 E2E 테스트는 환경 변수의 개발용 자격 증명으로 Basic 헤더를 붙인다.
  운영용 Bearer 인증은 계약에 정의만 해 두고 별도 기능에서 구현한다.
- **후속 확인 필요**: 운영 환경의 실제 인증 방식(SSO, JWT 등)은 별도 기능에서 결정하며, 이 기능은 역할
  확인 지점만 제공한다.

## 10. 관측 가능성 (헌법 IV)

- **결정**: JSON 구조화 로그와 요청 ID(MDC)를 사용하고, 생성·수정·삭제·충돌·검증 실패·업로드 거부를 로그로
  남긴다. 로그에는 비밀 정보와 인증 토큰을 포함하지 않는다. 오류 응답은 RFC 9457 형식의 Problem Details로
  통일하고 조치 가능한 한글 메시지를 담는다.
- **근거**: 헌법 IV(구조화된 로그, 조치 가능한 오류, 비밀·개인정보 금지)를 충족한다.

## 11. 로컬 개발 환경 (Docker Compose)

- **결정**: 루트 `docker-compose.yml` 하나로 구성한다.
  - 기본(프로필 없음): `postgres`(PostgreSQL 16, 헬스체크, 이름 있는 볼륨)만 실행한다. 백엔드와
    프런트엔드는 로컬에서 실행한다(Spring DevTools, Vite HMR, 디버거 연결).
  - `full` 프로필: `backend`, `frontend` 서비스를 추가해 `docker compose --profile full up`으로
    전체를 컨테이너로 실행한다. 이미지 업로드 파일은 이름 있는 볼륨에 저장한다.
  - 설정값(DB 계정·비밀번호, 개발용 운영자 계정)은 `.env`에서 읽고, 저장소에는 값 없는 `.env.example`만
    커밋한다. `.env`는 `.gitignore`에 포함한다.
- **근거**: 일상 개발은 핫 리로드가 빠른 로컬 실행이 유리하고(특히 Windows에서 컨테이너 파일 감시는 느림),
  온보딩·통합 확인에는 풀스택 컨테이너가 유용하다. 앱 Dockerfile은 `full` 프로필 전용으로 한정해 유지
  비용을 최소화한다. 비밀 정보 커밋 금지(헌법 기술·보안 제약)를 지킨다.
- **검토한 대안**: 전부 항상 Docker(핫 리로드·디버깅 불편), DB만 Docker(신규 팀원 환경 구성 부담).

## 12. 의존성 검토 (기술·보안 제약)

| 의존성 | 필요성 | 유지보수 상태 | 라이선스 |
|--------|--------|---------------|----------|
| Spring Boot 3.5 | 웹, JPA, 검증, 보안 기반. 계획의 3.3은 지원이 끝나 같은 3.x 계열의 최신 3.5.x를 사용한다 | 활발 | Apache 2.0 |
| PostgreSQL JDBC 드라이버(pgjdbc) | PostgreSQL 접속 | 활발 | BSD-2-Clause |
| JUnit 5, Mockito, AssertJ (`spring-boot-starter-test`) | 단위·통합 테스트 | 활발 | EPL 2.0 / MIT / Apache 2.0 |
| Gradle 래퍼 | 재현 가능한 빌드 | 활발 | Apache 2.0 |
| Flyway | 스키마 마이그레이션 | 활발 | Apache 2.0 (Community) |
| Testcontainers | 실제 PostgreSQL로 통합 테스트 | 활발 | MIT |
| springdoc-openapi | 코드와 계약 문서 일치 확인 | 활발 | Apache 2.0 |
| logstash-logback-encoder | JSON 구조화 로그 | 활발 | Apache 2.0 |
| React 18 / Vite | 운영자 웹 화면 | 활발 | MIT |
| TanStack Query | 서버 상태 조회·캐시·재시도 | 활발 | MIT |
| React Hook Form | 복잡한 상품 등록 폼 검증 | 활발 | MIT |
| Playwright | 핵심 사용자 여정 E2E | 활발 | Apache 2.0 |
| Spring Security | `PRODUCT_MANAGER` 역할 기반 접근 제어, 개발용 HTTP Basic | 활발 | Apache 2.0 |
| React Router | 목록·등록·수정·상세 화면 라우팅 | 활발 | MIT |
| Vitest, React Testing Library | 프런트엔드 단위·컴포넌트 테스트 | 활발 | MIT |
| Spotless + google-java-format, Checkstyle | 포맷·정적 분석 병합 게이트(헌법 I). 빌드 도구이며 배포물에 포함되지 않음 | 활발 | Apache 2.0 / LGPL 2.1 |
| ESLint, Prettier | 프런트엔드 린트·포맷 병합 게이트 | 활발 | MIT |
| swagger-request-validator | OpenAPI 계약 테스트(MockMvc) | 활발 | Apache 2.0 |

버전은 도입 시점(2026-10-07)의 최신 안정판을 쓴다: Spring Boot 3.5.16, Gradle 8.14.3, springdoc-openapi 2.9.1,
swagger-request-validator 2.46.1, logstash-logback-encoder 8.1.

**도입 절차**: 의존성은 이 표에 있는 것만 도입한다(헌법 "기술 및 보안 제약"). 표에 없는 의존성이 필요하면 도입
전에 이 표에 필요성·유지보수 상태·라이선스를 먼저 추가한다. 버전과 라이선스는 도입 시점에 다시 확인한다
(이 표는 계획 시점의 검토이며 외부 조회 결과가 아니다).

### 도입한 의존성의 실제 버전과 라이선스 (2026-10-07 확인)

라이선스는 Maven Central의 POM(필요하면 부모 POM)과 설치된 npm 패키지의 `package.json`에서 읽은 값이다.

| 구분 | 의존성 | 버전 | 라이선스 |
|------|--------|------|----------|
| 백엔드 | Spring Boot starters(web, data-jpa, validation, security) | 3.5.16 | Apache 2.0 |
| 백엔드 | Spring Security | 6.5.11 | Apache 2.0 |
| 백엔드 | Hibernate ORM | 6.6.53.Final | (Boot BOM이 관리, LGPL-2.1-or-later로 알려져 있으나 POM에서 직접 확인하지 않음) |
| 백엔드 | Flyway core, database-postgresql | 11.7.2 | Apache 2.0 |
| 백엔드 | PostgreSQL JDBC 드라이버 | 42.7.11 | BSD-2-Clause |
| 백엔드 | springdoc-openapi-starter-webmvc-api | 2.9.1 | Apache 2.0 |
| 백엔드 | logstash-logback-encoder | 8.1 | Apache 2.0 / MIT |
| 백엔드(테스트) | JUnit Jupiter | 5.12.2 | EPL 2.0 |
| 백엔드(테스트) | Mockito, AssertJ | 5.17.0, 3.27.7 | MIT, Apache 2.0 |
| 백엔드(테스트) | Testcontainers | 1.21.4 | MIT |
| 백엔드(테스트) | swagger-request-validator-mockmvc | 2.46.1 | Apache 2.0 |
| 빌드 도구 | Gradle 래퍼 | 8.14.3 | Apache 2.0 |
| 빌드 도구 | Spotless 플러그인, google-java-format | 8.10.3, **1.36.1** | Apache 2.0 |
| 빌드 도구 | Checkstyle | 14.3.0 | LGPL-2.1-or-later (배포물에 포함되지 않음) |
| 프런트엔드 | react, react-dom | 18.3.1 | MIT |
| 프런트엔드 | react-router, @tanstack/react-query, react-hook-form | 7.18.4, 5.104.1, 7.89.0 | MIT |
| 프런트엔드 | vite, vitest, @vitejs/plugin-react | 8.3.3, 5.0.3, 6.1.2 | MIT |
| 프런트엔드 | typescript | 6.0.3 | Apache 2.0 |
| 프런트엔드 | @playwright/test | 1.63.0 | Apache 2.0 |
| 프런트엔드 | eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, globals, prettier | 10.12.0, 10.0.1, 8.71.1, 7.1.1, 17.13.0, 3.9.9 | MIT |
| 프런트엔드 | @testing-library/react, user-event, jest-dom, jsdom | 16.3.3, 14.6.7, 7.0.1, 30.1.2 | MIT |
| 프런트엔드 | @types/node, @types/react, @types/react-dom | 26.6.4, 18.3.31, 18.3.7 | MIT |

- **계획과 다른 점**: Spring Boot는 3.3이 아니라 3.5.16이다(3.3은 지원이 끝남). google-java-format은 계획의 1.37.0이
  Spotless 8.10.3과 호환되지 않아(모든 파일에서 포맷 실패) 1.36.1을 쓴다.
- **사전 검토가 누락된 항목**: jsdom, user-event, jest-dom, typescript-eslint, @eslint/js, globals,
  eslint-plugin-react-hooks, @vitejs/plugin-react, typescript, @types/*는 위 검토 표에 없는 상태에서 프런트엔드
  구현 중에 설치되었다. 모두 이미 표에 있는 도구(React Testing Library, Vitest, ESLint, Vite)의 동반 패키지이고
  새 라이브러리 의존성은 아니지만, "표에 있는 의존성만 도입한다"는 절차(헌법 "기술 및 보안 제약")를 지키지 못했으므로 이
  표에 사후 기록한다. 이후에는 표를 먼저 갱신한 뒤 도입한다.

## 13. 고객 노출용 조회 API (FR-018)

- **결정**: 인증 없는 읽기 전용 `GET /api/v1/storefront/products`와 `/{productId}`를 제공한다. 판매중·품절만
  노출하고 숨김·삭제된 상품은 목록에서 제외하며 상세는 404로 처리한다. 응답에는 재고 수량 대신 `soldOut`
  여부만 담고 내부 필드(상품 코드, `version`, 변경 이력)는 포함하지 않는다. 운영자 API(`/api/v1/products/**`)는 계속
  `PRODUCT_MANAGER`만 접근할 수 있다.
- **근거**: "숨김 상품은 고객에게 노출되지 않는다", "수정이 1분 이내에 고객에게 반영된다"(SC-003)를 검증할
  대상이 필요하다. 별도 캐시 없이 DB를 직접 조회하므로 수정이 즉시 반영된다.
- **검토한 대안**: 고객 노출 규칙을 문서 규칙으로만 정의(검증 대상이 없어 테스트 불가).
