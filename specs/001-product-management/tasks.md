---

description: "커머스 상품 관리 기능 구현을 위한 작업 목록"
---

# 작업 목록 (Tasks): 커머스 상품 관리

**입력**: `/specs/001-product-management/`의 설계 문서

**선행 조건**: plan.md, spec.md, research.md, data-model.md, contracts/product-api.openapi.yaml, quickstart.md

**테스트**: 헌법 II(테스트 우선, 협상 불가)에 따라 모든 사용자 스토리에 테스트 작업을 포함한다. 각 스토리에서
테스트 작업을 먼저 작성하고 **실패하는 것을 확인한 뒤** 구현 작업을 시작한다. 버그 수정에는 회귀 테스트를 포함한다.

**구성**: 작업은 각 스토리를 독립적으로 구현하고 테스트할 수 있도록 사용자 스토리별로 묶는다.

## 형식: `[ID] [P?] [Story] 설명`

- **[P]**: 병렬 실행 가능 (서로 다른 파일, 미완료 작업에 대한 의존성 없음)
- **[Story]**: 이 작업이 속한 사용자 스토리 (US1~US4)
- 설명에 정확한 파일 경로를 포함한다

## 경로 규칙

- 백엔드 소스: `backend/src/main/java/com/example/commerce/` (아래에서 `{base}`로 줄여 쓴다)
- 백엔드 테스트: `backend/src/test/java/com/example/commerce/` (아래에서 `{test}`로 줄여 쓴다)
- 프런트엔드: `frontend/src/`, `frontend/tests/`
- 모든 문서·주석·커밋 메시지는 한글로 작성한다(헌법 VI). 식별자·경로·명령어는 원문을 유지한다

---

## Phase 1: 설정 (Setup, 공통 인프라)

**목적**: 프로젝트 초기화 및 기본 구조

- [X] T001 `specs/001-product-management/research.md` 12번 의존성 검토 표를 확정한다. 이 표에 있는 의존성만 도입하며, 표에 없는 의존성이 필요하면 도입 전에 필요성·유지보수 상태·라이선스를 먼저 추가한다 (헌법 "기술 및 보안 제약")
- [X] T002 `backend/build.gradle`, `backend/settings.gradle`에 Java 21 + Spring Boot 3.3 프로젝트 생성(Web, Data JPA, Validation, Security, Flyway, PostgreSQL 드라이버, springdoc-openapi, logstash-logback-encoder, Testcontainers, JUnit 5) 및 `{base}` 패키지 골격 생성
- [X] T003 `frontend/package.json`에 Vite + React 18 + TypeScript 프로젝트 생성(TanStack Query, React Hook Form, React Router, Vitest, React Testing Library, Playwright) 및 `frontend/src/` 디렉터리 골격 생성
- [X] T004 [P] `frontend/vite.config.ts`에 개발 서버의 `/api` 요청을 백엔드(`http://localhost:8080`)로 전달하는 프록시 설정 (CORS 설정 불필요, research.md 9번)
- [X] T005 [P] 저장소 루트에 `docker-compose.yml`(PostgreSQL 16, 헬스체크, 이름 있는 볼륨), `.env.example`(값 없이 키만: DB 계정·비밀번호, 개발용 운영자 계정·비밀번호), `.gitignore`(`.env` 포함) 작성
- [X] T006 [P] `backend/build.gradle`에 Spotless(google-java-format)와 Checkstyle 설정, `backend/config/checkstyle/checkstyle.xml` 작성. `./gradlew check`가 포맷·정적 분석을 수행하도록 연결
- [X] T007 [P] `frontend/eslint.config.js`, `frontend/.prettierrc`에 ESLint·Prettier 구성, `frontend/package.json`에 `lint`, `test`, `test:e2e` 스크립트 추가
- [X] T008 [P] `backend/src/main/resources/application.yml`, `application-dev.yml`에 환경 변수 기반 설정(DB 접속 정보, 이미지 저장 경로, 개발용 운영자 계정). 비밀 정보 값은 파일에 넣지 않는다
- [X] T009 [P] `backend/Dockerfile`(멀티 스테이지 빌드), `frontend/Dockerfile`(빌드 + 정적 서빙)을 작성하고 `docker-compose.yml`에 `backend`, `frontend` 서비스를 `full` 프로필로 추가(이미지 업로드는 이름 있는 볼륨)
- [ ] T010 [P] CI 워크플로를 구성한다: PR마다 `backend`에서 `./gradlew check`, `frontend`에서 `npm run lint && npm test`를 실행해 병합 게이트(헌법 "개발 워크플로 및 품질 게이트")를 자동으로 강제한다. 저장소 호스팅이 확정되면 해당 서비스의 설정 파일(예: `.github/workflows/ci.yml`)에 작성한다

---

## Phase 2: 기반 작업 (Foundational, 차단 선행 조건)

**목적**: 어떤 사용자 스토리든 구현하기 전에 반드시 완료되어야 하는 핵심 인프라

**⚠️ 중요**: 이 단계가 끝나기 전에는 어떤 사용자 스토리 작업도 시작할 수 없다

- [X] T011 `{test}/integration/IntegrationTestBase.java`에 Testcontainers(PostgreSQL 16) 기반 통합 테스트 공통 클래스 작성. 테스트 프로필의 Flyway 위치에 `db/test-migration`을 포함
- [X] T012 `backend/src/main/resources/db/migration/V1__create_schema.sql`에 data-model.md의 스키마 생성. 제약을 그대로 반영한다:
  - `categories`: id bigint PK, name varchar(100) NOT NULL UNIQUE
  - `products`: id bigint PK, code varchar(30) NOT NULL UNIQUE(삭제된 상품 포함 전체에서 고유), name varchar(200) NOT NULL, description text, price bigint NOT NULL CHECK (price > 0), stock_quantity integer CHECK (stock_quantity >= 0) NULL 허용(옵션 상품은 null), status NOT NULL IN (`ON_SALE`, `SOLD_OUT`, `HIDDEN`), category_id bigint NOT NULL FK, version bigint NOT NULL, created_at·updated_at timestamptz, deleted_at timestamptz NULL
  - `product_option_groups`: product_id FK, name varchar(50) NOT NULL (상품 내 고유), position integer; 옵션 값 테이블은 value varchar(50) NOT NULL (그룹 내 고유), position
  - `product_variants`: product_id FK, combination_key varchar(300) NOT NULL, stock_quantity integer NOT NULL CHECK (stock_quantity >= 0), UNIQUE (product_id, combination_key)
  - `product_images`: product_id FK, storage_key varchar(100) NOT NULL, content_type varchar(20) NOT NULL IN (`image/jpeg`, `image/png`), size_bytes integer NOT NULL CHECK (size_bytes <= 5242880), is_primary boolean NOT NULL, position integer
  - `product_change_logs`: product_id bigint NOT NULL, action IN (`CREATED`, `UPDATED`, `DELETED`), changed_by varchar(100) NOT NULL, changed_at timestamptz NOT NULL, summary jsonb
  - 상품 코드 자동 생성용 시퀀스 `product_code_seq`
- [X] T013 [P] `backend/src/main/resources/db/dev/R__seed_dev_categories.sql`에 개발 프로필 전용 샘플 카테고리 시드를 반복 실행 마이그레이션으로 작성하고(버전 순서와 무관하게 적용되어, 이후 추가되는 V3 같은 마이그레이션과 순서 오류가 나지 않는다) `application-dev.yml`의 Flyway locations에 연결
- [X] T014 [P] `backend/src/main/resources/db/migration/V2__seed_default_category.sql`에 기본 카테고리 "미분류"를 운영 환경에도 적용되는 마이그레이션으로 추가(name varchar(100) NOT NULL UNIQUE 준수, 이미 있으면 건너뜀). 카테고리 관리 기능이 제공되기 전에도 상품 등록이 막히지 않게 한다
- [X] T015 [P] `{test}/integration/SecurityIntegrationTest.java`에 인증 없음 → 401, 역할 없음 → 403, `PRODUCT_MANAGER` → 접근 허용 테스트 작성(실패 확인). 개발·테스트 프로필의 HTTP Basic 인증, `/api/v1/storefront/**`는 인증 없이 접근 가능, 운영자 API는 계속 보호됨 검증 포함 (FR-014, quickstart 16)
- [X] T016 `{base}/config/SecurityConfig.java`에 `/api/v1/**`를 `PRODUCT_MANAGER` 역할로 제한하는 보안 설정과, dev 프로필 인메모리 운영자(환경 변수에서 계정 주입) 구현. 개발·테스트 프로필은 HTTP Basic으로 인증하고, `/api/v1/storefront/**`는 인증 없이 허용한다 (T015 통과)
- [X] T017 [P] `{test}/integration/ErrorResponseTest.java`에 Problem Details(RFC 9457) 형식, `requestId` 포함, 한글 `detail`, 필드별 `errors` 검증 테스트 작성(실패 확인)
- [X] T018 `{base}/config/GlobalExceptionHandler.java`에 검증 실패 400, 존재하지 않음·삭제됨 404, 충돌 409, 파일 크기 초과 413을 Problem Details로 매핑 (T017 통과)
- [X] T019 [P] `{base}/config/RequestIdFilter.java`와 `backend/src/main/resources/logback-spring.xml`에 요청 ID(MDC) 부여 및 JSON 구조화 로그 구성. 인증 토큰과 비밀 정보는 로그에 남기지 않는다(헌법 IV)
- [X] T020 [P] `{test}/unit/ProductStatusPolicyTest.java`에 판매 상태 규칙 단위 테스트 작성(실패 확인): `HIDDEN`은 재고와 무관하게 유지, 운영자 지정 `ON_SALE`은 재고 합계 ≥ 1이면 `ON_SALE`·0이면 `SOLD_OUT`, `SOLD_OUT`은 재고 합계 ≥ 1로 보충되면 `ON_SALE`로 복귀, 옵션 상품의 재고 합계는 모든 옵션 조합의 합이며 옵션 없는 상품은 상품 재고 (FR-008, FR-009, FR-016)
- [X] T021 `{base}/product/domain/ProductStatusPolicy.java`에 판매 상태 결정 규칙 구현 (T020 통과)
- [X] T022 [P] `{base}/category/Category.java`, `CategoryRepository.java`에 Category 엔터티·리포지토리 생성 (name varchar(100) NOT NULL UNIQUE)
- [X] T023 [P] `{base}/product/domain/Product.java`에 Product 엔터티 생성: `@Version version`, `deleted_at` 기반 `@SQLRestriction("deleted_at IS NULL")`, `code`는 `updatable = false`, 코드 형식 `^[A-Z0-9-]{3,30}$`, price는 0 초과, stock_quantity는 0 이상
- [X] T024 [P] `{base}/product/domain/ProductOptionGroup.java`, `ProductOptionValue.java`, `ProductVariant.java`, `ProductImage.java`에 옵션 그룹·옵션 값·옵션 조합·이미지 엔터티 생성 (data-model.md 제약 준수)
- [X] T025 `{base}/product/repository/ProductRepository.java`에 상품 리포지토리 생성 (T023, T024에 의존)
- [X] T026 [P] `{test}/integration/ChangeLogRecorderTest.java`에 변경 이력이 상품 변경과 같은 트랜잭션에서 저장되고(상품 저장 실패 시 이력도 롤백), 변경자·시각·작업 종류·요약이 기록되는지 테스트 작성(실패 확인) (FR-013)
- [X] T027 `{base}/audit/ProductChangeLog.java`, `ProductChangeLogRepository.java`, `ChangeLogRecorder.java`에 추가 전용 변경 이력 엔터티와 기록기 구현 (T026 통과)
- [X] T028 [P] `frontend/src/services/apiClient.ts`, `frontend/src/App.tsx`, `frontend/src/main.tsx`에 API 클라이언트(개발용 자격 증명 환경 변수로 Basic 헤더 부착, 오류 Problem Details 파싱), 라우팅, TanStack Query 설정 작성

**체크포인트**: 기반 준비 완료 - 이제 사용자 스토리 구현을 시작할 수 있다

---

## Phase 3: 사용자 스토리 1 - 상품 등록 및 조회 (우선순위: P1) 🎯 MVP

**목표**: 운영자가 옵션·이미지를 포함한 상품을 등록하고 목록과 상세를 확인한다.

**독립 테스트**: 필수 정보를 입력해 상품을 저장한 뒤, 목록과 상세 화면에서 같은 정보가 보이면 통과한다 (quickstart 1~6).

### 사용자 스토리 1 테스트 (먼저 작성하고 실패를 확인할 것) ⚠️

- [X] T029 [P] [US1] `{test}/contract/ProductCreateContractTest.java`에 `POST /products`, `GET /products/{productId}`, `GET /products`(기본 페이지), `POST /products/{productId}/images`, `GET /categories`가 `contracts/product-api.openapi.yaml`과 일치하는지 검증하는 계약 테스트 작성 (OpenAPI 검증 라이브러리는 research.md 12번 표의 swagger-request-validator 사용)
- [X] T030 [P] [US1] `{test}/integration/ProductRegistrationIntegrationTest.java`에 quickstart 시나리오 1~5 작성: 등록 성공(코드 `PRD-`로 자동 생성), 필수 항목(상품명·가격·카테고리) 누락 시 400과 누락 항목 안내, 가격 0과 재고 -1 거부, 중복 코드 409, 옵션(색상×사이즈) 등록과 같은 옵션 조합 중복 400
- [X] T031 [P] [US1] `{test}/integration/ProductImageUploadIntegrationTest.java`에 quickstart 시나리오 6 작성: 11장째 업로드 거부, 6MB 파일 413, GIF 거부, 확장자만 `.png`인 위장 파일 거부, 이미지 없이 등록한 상품도 허용, 대표 이미지는 이미지가 있으면 상품당 정확히 하나, 업로드가 변경 이력(`UPDATED`)에 기록됨 (FR-002)
- [X] T032 [P] [US1] `{test}/unit/ProductCodeGeneratorTest.java`, `{test}/unit/VariantCombinationKeyTest.java`, `{test}/unit/ImageValidatorTest.java`에 코드 형식 `^[A-Z0-9-]{3,30}$`, 조합 키 정규화·모든 조합 누락 검출, 파일 시그니처(JPG·PNG) 검증 단위 테스트 작성
- [X] T033 [P] [US1] `frontend/tests/unit/ProductForm.test.tsx`에 필수 항목 누락 안내, 가격·재고 입력 검증, 옵션 조합 표 생성 컴포넌트 테스트 작성

### 사용자 스토리 1 구현

- [X] T034 [P] [US1] `{base}/product/service/ProductCodeGenerator.java`에 `product_code_seq` 기반 `PRD-000001` 형식 코드 생성과 직접 입력 코드 형식 검증 구현
- [X] T035 [P] [US1] `{base}/product/domain/VariantCombinationKey.java`에 옵션 값 조합을 정규화한 키(예: `색상=빨강|사이즈=M`) 생성과 모든 조합 포함 여부 검증 구현
- [X] T036 [P] [US1] `{base}/image/ImageStorage.java`(인터페이스), `LocalImageStorage.java`(서버 생성 UUID 파일명으로 로컬 디렉터리에 저장), `ImageValidator.java`(파일 시그니처로 JPG·PNG 판별, 장당 5MB 이하) 구현
- [X] T037 [P] [US1] `{base}/product/api/ProductCreateRequest.java`, `ProductResponse.java`, `ProductSummaryResponse.java`, `ProductPageResponse.java`, `OptionGroupDto.java`, `VariantDto.java`에 계약의 요청/응답 DTO와 Bean Validation 작성 (name 1~200자, price 1 이상, stockQuantity 0 이상, 옵션 값 중복 불가)
- [X] T038 [US1] `{base}/product/service/ProductCreateService.java`에 `create` 구현: 코드 생성/중복 검사, 옵션 상품은 모든 조합의 재고 검증, 옵션 상품은 상품 재고 null, `ProductStatusPolicy`로 상태 결정, `CREATED` 변경 이력 기록(`ChangeLogRecorder`) (T021, T025, T027, T034, T035에 의존)
- [X] T039 [US1] `{base}/image/ProductImageUploadService.java`에 업로드 구현: 상품당 10장 초과 거부, 첫 이미지는 자동으로 대표 지정, 저장 실패 시 DB 반영 취소, 업로드를 `UPDATED` 변경 이력에 기록 (T036, T038에 의존)
- [X] T040 [US1] `{base}/product/service/ProductQueryService.java`에 상세 조회와 기본 목록 조회(`page`/`size`, 기본 20·최대 100, 기본 정렬 `createdAt,desc`) 구현
- [X] T041 [US1] `{base}/product/api/ProductCreateController.java`(POST), `{base}/product/api/ProductQueryController.java`(GET 목록·상세), `{base}/image/ProductImageController.java`(업로드), `{base}/category/CategoryController.java`에 `POST /api/v1/products`(201 + Location), `GET /api/v1/products`, `GET /api/v1/products/{productId}`, `POST /api/v1/products/{productId}/images`, `GET /api/v1/categories` 구현 (T037~T040에 의존)
- [X] T042 [US1] `{base}/product/service/ProductCreateService.java`, `{base}/image/ProductImageUploadService.java`에 생성·업로드 거부·검증 실패의 구조화 로그 추가 (조치 가능한 맥락 포함, 비밀·개인정보 제외) (헌법 IV)
- [X] T043 [P] [US1] `frontend/src/services/productApi.ts`, `categoryApi.ts`, `frontend/src/hooks/useProducts.ts`, `useCategories.ts`에 API 호출과 쿼리 훅 작성
- [X] T044 [P] [US1] `frontend/src/components/OptionEditor.tsx`, `ImageUploader.tsx`에 옵션 그룹·값 편집과 조합별 재고 표, 이미지 선택(JPG·PNG, 10장, 5MB 사전 검증)·대표 지정 UI 작성. 재고가 0인 옵션 조합은 품절로 표시
- [X] T045 [US1] `frontend/src/pages/ProductCreatePage.tsx`에 상품 등록 화면(React Hook Form, 필수 항목 누락 안내, 서버 `errors` 표시, 등록 후 이미지 업로드가 실패하면 상품은 이미지 없이 유지되고 수정 화면에서 다시 올리도록 안내) 작성 (T043, T044에 의존)
- [X] T046 [US1] `frontend/src/pages/ProductListPage.tsx`, `ProductDetailPage.tsx`에 목록(페이지 이동)과 상세 화면(재고가 0인 옵션 조합은 품절로 표시) 작성 (T043에 의존)
- [X] T047 [P] [US1] `frontend/tests/e2e/auth.setup.ts`, `frontend/playwright.config.ts`에 E2E 공통 인증 설정(환경 변수의 개발용 자격 증명으로 Basic 헤더 부착) 작성
- [X] T048 [US1] `frontend/tests/e2e/register-product.spec.ts`에 등록 → 목록 → 상세 확인 E2E 시나리오 작성 및 통과 확인

**체크포인트**: 이 시점에서 사용자 스토리 1은 완전히 동작하고 독립적으로 테스트 가능해야 한다 (MVP)

---

## Phase 4: 사용자 스토리 2 - 상품 정보 수정 및 판매 상태 관리 (우선순위: P2)

**목표**: 운영자가 상품 정보와 재고를 수정하고 판매 상태를 관리한다. 동시 수정은 충돌로 감지한다.

**독립 테스트**: 기존 상품의 가격·재고·판매 상태를 바꾼 뒤 목록과 상세에 반영되고, 동시 수정은 거부되는지 확인한다 (quickstart 7~10).

### 사용자 스토리 2 테스트 (먼저 작성하고 실패를 확인할 것) ⚠️

- [X] T049 [P] [US2] `{test}/contract/ProductUpdateContractTest.java`에 `PUT /products/{productId}`, `DELETE /products/{productId}/images/{imageId}`, `PUT /products/{productId}/images/{imageId}/primary`가 계약과 일치하는지 검증하는 계약 테스트 작성
- [X] T050 [P] [US2] `{test}/integration/ProductUpdateIntegrationTest.java`에 quickstart 시나리오 7~10 작성: 모든 조합 재고 0 → `SOLD_OUT`, 재고 보충 → `ON_SALE` 자동 복귀, `HIDDEN`은 재고를 보충해도 유지, 같은 `version`으로 두 번 저장하면 두 번째는 409, 수정 요청이 상품 코드를 바꾸지 못함, 일부 옵션 조합만 재고 0이면 상품은 판매중이고 해당 조합만 품절로 표시, `UPDATED` 변경 이력 기록
- [X] T051 [P] [US2] `{test}/integration/ProductImageManageIntegrationTest.java`에 이미지 삭제(마지막 한 장도 삭제 가능, 대표를 삭제하면 표시 순서가 가장 앞선 남은 이미지가 대표가 됨), 대표 이미지 변경 시 대표가 정확히 하나로 유지되는지, 이미지 삭제·대표 변경이 변경 이력(`UPDATED`)에 기록되는지 테스트 작성
- [X] T052 [P] [US2] `frontend/tests/unit/ProductEditConflict.test.tsx`에 409 응답 시 최신 내용을 다시 불러오라는 안내가 표시되는지 컴포넌트 테스트 작성

### 사용자 스토리 2 구현

- [X] T053 [P] [US2] `{base}/product/api/ProductUpdateRequest.java`에 `version`(필수) 포함 수정 요청 DTO 작성 (상태는 `ON_SALE`·`HIDDEN`만 허용)
- [X] T054 [US2] `{base}/product/service/ProductUpdateService.java`에 `update` 구현: 낙관적 락 충돌을 409로 변환, 코드 변경 불가, 옵션·조합 재구성, `ProductStatusPolicy` 재적용, `UPDATED` 이력에 변경 필드와 이전/이후 값 기록 (T053에 의존)
- [X] T055 [US2] `{base}/image/ProductImageManageService.java`에 이미지 삭제(마지막 한 장도 삭제 가능, 대표 삭제 시 남은 첫 이미지를 대표로 승격)와 대표 이미지 지정 구현, 각 변경을 `UPDATED` 변경 이력에 기록
- [X] T056 [US2] `{base}/product/api/ProductUpdateController.java`, `{base}/image/ProductImageManageController.java`에 `PUT /api/v1/products/{productId}`, `DELETE .../images/{imageId}`, `PUT .../images/{imageId}/primary` 구현 (T054, T055에 의존)
- [X] T057 [US2] `{base}/product/service/ProductUpdateService.java`에 수정 성공·충돌·검증 실패의 구조화 로그 추가 (헌법 IV)
- [X] T058 [P] [US2] `frontend/src/components/ConflictNotice.tsx`에 409 시 "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러오세요" 안내와 다시 불러오기 동작 작성
- [X] T059 [US2] `frontend/src/pages/ProductEditPage.tsx`에 수정 화면(`version` 전송, 판매 상태 지정은 판매중·숨김만, 품절은 재고 기반 표시) 작성 (T058에 의존)
- [X] T060 [US2] `frontend/tests/e2e/edit-product.spec.ts`에 가격 수정·재고 0 → 품절·재고 보충 → 판매중 복귀·충돌 안내 E2E 시나리오 작성 및 통과 확인

### 사용자 스토리 2 - 고객 노출 조회 (FR-018, 먼저 테스트 작성)

- [X] T061 [P] [US2] `{test}/contract/StorefrontContractTest.java`에 `GET /storefront/products`, `GET /storefront/products/{productId}`가 계약과 일치하는지(인증 불필요, 응답에 재고 수량·코드·`version` 없음) 검증하는 계약 테스트 작성
- [X] T062 [P] [US2] `{test}/integration/StorefrontIntegrationTest.java`에 quickstart 시나리오 17~18 작성: 판매중·품절만 목록에 노출, 숨김·삭제 상품은 목록 제외와 상세 404, 응답에 `soldOut`만 있고 재고 수량 없음, 가격 수정 직후 고객 조회에 즉시 반영 (SC-003)
- [X] T063 [US2] `{base}/storefront/StorefrontQueryService.java`, `StorefrontController.java`, `StorefrontProductResponse.java`에 `GET /api/v1/storefront/products`(`categoryId`, `page`, `size`)와 `GET /api/v1/storefront/products/{productId}` 구현: 숨김·삭제 제외, 재고 대신 `soldOut` 반환, 캐시 없이 DB 직접 조회 (T061, T062 통과)

**체크포인트**: 이 시점에서 사용자 스토리 1과 2가 모두 독립적으로 동작해야 한다

---

## Phase 5: 사용자 스토리 3 - 상품 검색, 필터링 및 정렬 (우선순위: P3)

**목표**: 운영자가 상품명·카테고리·판매 상태로 상품을 찾고 등록일·가격으로 정렬한다.

**독립 테스트**: 여러 상품을 등록한 뒤 검색어·카테고리·상태 조건이 적용되고, 결과가 없으면 "결과 없음"이 표시되는지 확인한다 (quickstart 11~12).

### 사용자 스토리 3 테스트 (먼저 작성하고 실패를 확인할 것) ⚠️

- [X] T064 [P] [US3] `{test}/contract/ProductListContractTest.java`에 `GET /products`의 `q`, `categoryId`, `status`, `sort`, `page`, `size` 파라미터와 빈 결과 응답이 계약과 일치하는지 검증하는 계약 테스트 작성
- [X] T065 [P] [US3] `{test}/integration/ProductSearchIntegrationTest.java`에 상품명 부분 일치, 카테고리·상태 필터 조합, 등록일·가격 오름/내림차순 정렬, 페이지 경계(`size` 최대 100), 조건에 맞는 상품이 없으면 빈 `content` 반환 테스트 작성
- [X] T066 [P] [US3] `{test}/integration/ProductSearchPerformanceTest.java`에 상품 1만 개 적재 후 검색 응답 p95 1초 미만을 확인하는 테스트 작성 (SC-002, quickstart 12)
- [X] T067 [P] [US3] `frontend/tests/unit/ProductListFilters.test.tsx`에 검색어·필터·정렬 변경 시 쿼리 파라미터 반영과 "결과 없음" 안내 표시 컴포넌트 테스트 작성

### 사용자 스토리 3 구현

- [X] T068 [US3] `backend/src/main/resources/db/migration/V3__add_search_indexes.sql`에 `pg_trgm` 확장과 `products.name` GIN 인덱스, `(status, category_id)`, `created_at`, `price` 인덱스 추가
- [X] T069 [P] [US3] `{base}/product/repository/ProductSpecifications.java`에 상품명 부분 일치(대소문자 무시), 카테고리, 판매 상태 조건 조합용 `Specification` 구현
- [X] T070 [US3] `{base}/product/service/ProductQueryService.java`, `{base}/product/api/ProductQueryController.java`에 `q`, `categoryId`, `status`, `sort`(`createdAt,desc|asc`, `price,desc|asc`) 처리 추가 (T068, T069에 의존)
- [X] T071 [US3] `{base}/product/service/ProductQueryService.java`에 검색 조건·결과 수·소요 시간 구조화 로그 추가 (헌법 IV)
- [X] T072 [P] [US3] `frontend/src/components/ProductFilters.tsx`에 검색어 입력, 카테고리·판매 상태 선택, 정렬 선택 UI 작성
- [X] T073 [US3] `frontend/src/pages/ProductListPage.tsx`에 필터·정렬 연결과 "결과 없음" 안내 추가 (T072에 의존)
- [X] T074 [US3] `frontend/tests/e2e/search-products.spec.ts`에 검색·필터·정렬 E2E 시나리오 작성 및 통과 확인

**체크포인트**: 이제 사용자 스토리 1~3이 모두 독립적으로 동작해야 한다

---

## Phase 6: 사용자 스토리 4 - 상품 삭제 (우선순위: P3)

**목표**: 운영자가 상품을 삭제(논리 삭제)하고, 과거 주문 정보는 보존한다.

**독립 테스트**: 삭제 확인 후 상품이 목록에서 사라지고 상세는 404이며, 데이터는 보존되는지 확인한다 (quickstart 13~15).

### 사용자 스토리 4 테스트 (먼저 작성하고 실패를 확인할 것) ⚠️

- [X] T075 [P] [US4] `{test}/contract/ProductDeleteContractTest.java`에 `DELETE /products/{productId}`의 204·404 응답이 계약과 일치하는지 검증하는 계약 테스트 작성
- [X] T076 [P] [US4] `backend/src/test/resources/db/test-migration/V9000__test_order_item.sql`에 상품을 참조하는 테스트 전용 주문 항목 테이블 작성(과거 주문 정보 보존 검증용, 운영 스키마에는 포함하지 않음)
- [X] T077 [P] [US4] `{test}/integration/ProductDeleteIntegrationTest.java`에 quickstart 시나리오 13~15 작성: 삭제 후 목록에서 제외, 상세 404, `deleted_at` 기록, 주문 이력이 있는 상품을 삭제해도 참조 데이터 보존, 삭제된 상품의 코드로 재등록 시 409, `DELETED` 변경 이력 기록, 이미 삭제된 상품 재삭제 404
- [X] T078 [P] [US4] `frontend/tests/unit/DeleteConfirmDialog.test.tsx`에 삭제 요청 시 확인 대화상자가 표시되고, 취소하면 삭제 요청이 전송되지 않는지 컴포넌트 테스트 작성

### 사용자 스토리 4 구현

- [X] T079 [US4] `{base}/product/service/ProductDeleteService.java`에 `delete` 구현: `deleted_at` 기록, `DELETED` 변경 이력 기록, 하위 데이터(옵션·이미지 메타데이터) 보존 (테스트 환경: T011의 Flyway 테스트 위치와 T076의 테스트 주문 테이블)
- [X] T080 [US4] `{base}/product/api/ProductDeleteController.java`에 `DELETE /api/v1/products/{productId}`(204) 구현 (T079에 의존)
- [X] T081 [US4] `{base}/product/service/ProductDeleteService.java`에 삭제 로그 추가 (대상 상품 ID·요청자, 헌법 IV)
- [X] T082 [P] [US4] `frontend/src/components/DeleteConfirmDialog.tsx`에 삭제 확인 대화상자 작성
- [X] T083 [US4] `frontend/src/pages/ProductDetailPage.tsx`에 삭제 버튼과 확인 절차, 삭제 후 목록 이동 추가 (T082에 의존)
- [X] T084 [US4] `frontend/tests/e2e/delete-product.spec.ts`에 삭제 확인 → 목록에서 사라짐 → 상세 접근 404 안내 E2E 시나리오 작성 및 통과 확인

**체크포인트**: 모든 사용자 스토리가 독립적으로 동작해야 한다

---

## Phase 7: 마무리 및 공통 관심사 (Polish & Cross-Cutting Concerns)

**목적**: 여러 사용자 스토리에 영향을 미치는 개선 사항

- [X] T085 [P] 저장소 루트 `README.md`에 한글로 프로젝트 개요, 로컬 실행(방식 A·B), 테스트 실행 방법 작성 (헌법 VI)
- [X] T086 [P] `{test}/integration/QuickstartScenariosTest.java`에 quickstart 19개 시나리오와 테스트의 대응 관계를 확인하는 추적 테스트(누락된 시나리오 검출) 작성
- [X] T087 보안 점검: 이미지 업로드의 경로 조작·위장 파일, 모든 엔드포인트의 권한, 로그의 비밀·개인정보 노출 여부를 점검하고 발견 사항은 회귀 테스트와 함께 수정 (헌법 기술·보안 제약)
- [X] T088 [P] `specs/001-product-management/research.md` 12번 의존성 표를 실제 도입한 의존성의 버전·라이선스와 대조해 갱신(표에 없는 의존성이 도입되지 않았는지 확인)
- [X] T089 코드 정리와 리팩터링: 사용되지 않는 코드·사유 없는 린트 무시 제거 (헌법 I)
- [X] T090 `backend`에서 `./gradlew check`, `frontend`에서 `npm run lint && npm test && npm run test:e2e`를 실행해 병합 게이트를 확인
- [X] T091 quickstart.md의 방식 A·B 실행 절차와 19개 검증 시나리오를 실제로 실행해 검증하고 어긋나는 문서를 수정

---

## 의존성 및 실행 순서

### 단계 의존성

- **설정 (Phase 1)**: 의존성 없음 - 즉시 시작 가능
- **기반 작업 (Phase 2)**: 설정 완료에 의존 - 모든 사용자 스토리를 차단
- **사용자 스토리 (Phase 3~6)**: 모두 기반 작업 단계 완료에 의존
  - 이후 사용자 스토리는 병렬로 진행 가능 (인력이 있는 경우)
  - 또는 우선순위 순서로 순차 진행 (US1 → US2 → US3 → US4)
- **마무리 (Phase 7)**: 원하는 모든 사용자 스토리의 완료에 의존

### 사용자 스토리 의존성

- **US1 (P1)**: 기반 작업 이후 시작 가능 - 다른 스토리에 의존하지 않음
- **US2 (P2)**: 기반 작업 이후 시작 가능 - 수정할 상품을 만들기 위해 US1의 등록 API를 테스트 준비 단계에서 사용하지만, 독립적으로 테스트 가능해야 함
- **US3 (P3)**: 기반 작업 이후 시작 가능 - US1의 기본 목록 조회(T040)를 확장하므로 T040 이후에 시작
- **US4 (P3)**: 기반 작업 이후 시작 가능 - 상세 화면(T046)에 버튼을 추가하므로 T083는 T046 이후에 시작

### 각 사용자 스토리 내부

- 테스트는 반드시 구현 전에 작성하고 실패를 확인해야 한다 (헌법 II)
- 엔터티·정책 → 서비스 → 컨트롤러 → 로그 → 프런트엔드
- 핵심 구현 → 통합 → E2E
- 스토리를 완료한 뒤 다음 우선순위로 이동

### 병렬 실행 기회

- Phase 1에서 [P] 표시 작업(T005~T009)은 모두 병렬 실행 가능
- Phase 2에서 [P] 표시 테스트·엔터티·프런트엔드 기반 작업은 병렬 실행 가능
- 기반 작업이 끝나면 US1과 US3의 테스트 작성, US4의 테스트·테스트 마이그레이션 작성을 병렬로 시작 가능
- 한 사용자 스토리의 [P] 표시 테스트는 모두 병렬 실행 가능
- 백엔드와 프런트엔드 작업은 서로 다른 디렉터리이므로 계약(OpenAPI)이 고정된 이후 병렬 진행 가능

---

## 병렬 실행 예시: 사용자 스토리 1

```bash
# 사용자 스토리 1의 모든 테스트를 함께 작성:
Task: "T029 ProductCreateContractTest 작성"
Task: "T030 ProductRegistrationIntegrationTest 작성"
Task: "T031 ProductImageUploadIntegrationTest 작성"
Task: "T032 코드 생성·조합 키·이미지 검증 단위 테스트 작성"
Task: "T033 ProductForm 컴포넌트 테스트 작성"

# 테스트 실패를 확인한 뒤, 서로 독립적인 구성 요소를 함께 구현:
Task: "T034 ProductCodeGenerator 구현"
Task: "T035 VariantCombinationKey 구현"
Task: "T036 ImageStorage·ImageValidator 구현"
Task: "T037 요청/응답 DTO 작성"
```

---

## 구현 전략

### MVP 우선 (사용자 스토리 1만)

1. Phase 1 완료: 설정
2. Phase 2 완료: 기반 작업 (중요 - 모든 스토리를 차단)
3. Phase 3 완료: 사용자 스토리 1
4. **중단 및 검증**: 사용자 스토리 1을 독립적으로 테스트 (quickstart 1~6)
5. 준비되었으면 시연

### 점진적 전달

1. 설정 + 기반 작업 완료 → 기반 준비 완료
2. 사용자 스토리 1 추가 → 독립 테스트 → 시연 (MVP!)
3. 사용자 스토리 2 추가 → 독립 테스트 → 시연
4. 사용자 스토리 3 추가 → 독립 테스트 → 시연
5. 사용자 스토리 4 추가 → 독립 테스트 → 시연
6. 각 스토리는 이전 스토리를 깨뜨리지 않고 가치를 더한다

---

## 참고 (Notes)

- [P] 작업 = 서로 다른 파일, 의존성 없음
- [Story] 라벨은 추적성을 위해 작업을 특정 사용자 스토리에 연결한다
- 각 사용자 스토리는 독립적으로 완료하고 테스트할 수 있어야 한다
- 구현 전에 테스트가 실패하는지 확인한다 (헌법 II, 협상 불가)
- 각 작업 또는 논리적 그룹 단위로 한글 메시지로 커밋한다
- 어떤 체크포인트에서든 멈추고 스토리를 독립적으로 검증할 수 있다
- 피할 것: 모호한 작업, 같은 파일 충돌, 독립성을 깨는 스토리 간 의존성
