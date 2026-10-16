# 데이터 모델 (Data Model): 커머스 상품 관리

**날짜**: 2026-10-07 | **명세**: [spec.md](./spec.md) | **조사**: [research.md](./research.md)

## 엔터티

### Category (카테고리)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| name | varchar(100) | 필수, 고유 |

- 카테고리는 미리 정의되어 있거나 별도로 관리된다(가정). 이 기능은 조회와 상품 지정만 한다.
- 관계: 하나의 카테고리는 여러 상품을 가진다 (1:N).

### Product (상품)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| code | varchar(30) | 필수, 삭제된 상품 포함 전체에서 고유(FR-005), 생성 후 변경 불가, 형식 `^[A-Z0-9-]{3,30}$` |
| name | varchar(200) | 필수(FR-003), 중복 허용 |
| description | text | 선택 |
| price | bigint | 필수, 0 초과 정수(원)(FR-004) |
| stock_quantity | integer | 옵션이 없는 상품에서만 사용, 0 이상(FR-004). 옵션 상품은 null |
| status | enum | `ON_SALE`, `SOLD_OUT`, `HIDDEN` |
| category_id | bigint (FK) | 필수(FR-003) |
| version | bigint | 낙관적 락 버전(FR-007) |
| created_at / updated_at | timestamptz | 자동 기록 |
| deleted_at | timestamptz | null이면 정상, 값이 있으면 논리 삭제됨(FR-012) |

- 관계: 카테고리 1:N 상품, 상품 1:N 옵션 그룹, 상품 1:N 옵션 조합, 상품 1:N 이미지.
- 인덱스: `code` 고유 인덱스, `name`에 `pg_trgm` GIN 인덱스, `(status, category_id)`, `created_at`, `price`.

### ProductOptionGroup (옵션 그룹)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| product_id | bigint (FK) | 필수 |
| name | varchar(50) | 필수, 상품 내 고유 (예: 색상) |
| 옵션 값 | ProductOptionValue 1:N | 1개 이상, 그룹 내 고유 (예: 빨강, 파랑) |
| position | integer | 표시 순서 |

### ProductOptionValue (옵션 값)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| option_group_id | bigint (FK) | 필수 |
| value | varchar(50) | 필수, 옵션 그룹 내 고유 (예: 빨강) |
| position | integer | 표시 순서 |

- 옵션 그룹 1:N 옵션 값. 옵션 조합(ProductVariant)은 각 옵션 그룹에서 옵션 값을 하나씩 고른 조합이다.

### ProductVariant (옵션 조합)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| product_id | bigint (FK) | 필수 |
| combination_key | varchar(300) | 옵션 값 조합을 정규화한 키(예: `색상=빨강|사이즈=M`), `(product_id, combination_key)` 고유 |
| stock_quantity | integer | 필수, 0 이상 |

- 옵션 그룹이 있는 상품은 모든 옵션 값의 조합마다 하나의 Variant를 가진다. 가격은 Variant에 두지 않는다(FR-015).

### ProductImage (상품 이미지)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| product_id | bigint (FK) | 필수 |
| storage_key | varchar(100) | 서버가 생성한 UUID 파일명 |
| content_type | varchar(20) | `image/jpeg` 또는 `image/png` |
| size_bytes | integer | 5MB(5,242,880바이트) 이하 |
| is_primary | boolean | 상품당 정확히 하나가 true(이미지가 있는 경우) |
| position | integer | 표시 순서 |

- 상품당 이미지는 0장 이상 10장 이하(FR-002). 이미지 없이도 상품을 등록할 수 있다.
- 대표 이미지를 삭제하면 표시 순서가 가장 앞선 남은 이미지가 대표가 된다.

### ProductChangeLog (변경 이력)

| 필드 | 타입 | 규칙 |
|------|------|------|
| id | bigint (PK) | 자동 생성 |
| product_id | bigint | 대상 상품(삭제된 상품도 참조 가능) |
| action | enum | `CREATED`, `UPDATED`, `DELETED` |
| changed_by | varchar(100) | 변경한 운영자 식별자 |
| changed_at | timestamptz | 변경 시각 |
| summary | jsonb | 변경된 필드와 이전/이후 값 |

- 상품 변경과 같은 트랜잭션에서 기록한다(FR-013). 수정·삭제할 수 없는 추가 전용 기록이다.

## 검증 규칙 요약

| 규칙 | 근거 |
|------|------|
| 상품명·가격·카테고리 필수, 누락 항목 안내 | FR-003 |
| 가격 > 0, 재고 ≥ 0 정수 | FR-004 |
| 코드 고유, 비우면 자동 생성, 생성 후 변경 불가 | FR-005 |
| 이미지 JPG·PNG, 장당 5MB, 상품당 10장 | FR-002 |
| 옵션 조합 중복 금지 | 예외 상황 |
| 수정 시 `version` 불일치면 거부 | FR-007 |

## 상태 전이 (판매 상태)

상태는 저장 시마다 다음 규칙으로 결정한다(research.md 4번).

```text
운영자 지정 HIDDEN  ──────────────▶ HIDDEN  (재고 변화와 무관하게 유지)
운영자 지정 ON_SALE ──┬─ 재고 합계 ≥ 1 ─▶ ON_SALE
                      └─ 재고 합계 = 0 ─▶ SOLD_OUT

SOLD_OUT ── 재고 합계 ≥ 1로 보충 ─▶ ON_SALE (자동 복귀)
ON_SALE  ── 재고 합계 = 0으로 감소 ─▶ SOLD_OUT (자동 전환)
```

- 재고 합계: 옵션 상품은 모든 Variant의 합, 옵션 없는 상품은 `stock_quantity`.
- `HIDDEN`에서 `ON_SALE`로 바꾸면 위 규칙에 따라 재고가 0이면 곧바로 `SOLD_OUT`이 된다.

## 삭제 (논리 삭제)

- 삭제 요청 시 `deleted_at`을 기록하고 `DELETED` 이력을 남긴다. 이후 운영자 목록·상세·고객 노출에서
  제외되며, 삭제된 상품의 상세 접근은 `404`를 반환한다(예외 상황).
- 삭제된 행과 그 하위 데이터(옵션, 이미지 메타데이터)는 보존되어 과거 주문 정보 조회에 쓰인다.
