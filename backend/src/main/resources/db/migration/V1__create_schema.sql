-- 상품 관리 스키마 (data-model.md 기준)

CREATE TABLE categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 상품 코드 자동 생성용 시퀀스
CREATE SEQUENCE product_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE products (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- 삭제된 상품을 포함해 전체에서 고유하다(UNIQUE는 논리 삭제된 행에도 적용된다).
    code           VARCHAR(30)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9-]{3,30}$'),
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    price          BIGINT       NOT NULL CHECK (price > 0),
    -- 옵션이 없는 상품만 사용한다. 옵션 상품은 NULL.
    stock_quantity INTEGER      CHECK (stock_quantity >= 0),
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'HIDDEN')),
    category_id    BIGINT       NOT NULL REFERENCES categories (id),
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE product_option_groups (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT      NOT NULL REFERENCES products (id),
    name       VARCHAR(50) NOT NULL,
    position   INTEGER     NOT NULL,
    UNIQUE (product_id, name)
);

CREATE TABLE product_option_values (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    option_group_id BIGINT      NOT NULL REFERENCES product_option_groups (id),
    value           VARCHAR(50) NOT NULL,
    position        INTEGER     NOT NULL,
    UNIQUE (option_group_id, value)
);

CREATE TABLE product_variants (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products (id),
    combination_key VARCHAR(300) NOT NULL,
    stock_quantity  INTEGER      NOT NULL CHECK (stock_quantity >= 0),
    UNIQUE (product_id, combination_key)
);

CREATE TABLE product_images (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id   BIGINT       NOT NULL REFERENCES products (id),
    storage_key  VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(20)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    size_bytes   INTEGER      NOT NULL CHECK (size_bytes <= 5242880),
    is_primary   BOOLEAN      NOT NULL,
    position     INTEGER      NOT NULL
);

-- 변경 이력은 추가 전용이다. 삭제된 상품도 참조할 수 있도록 FK를 두지 않는다.
CREATE TABLE product_change_logs (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    action     VARCHAR(10)  NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'DELETED')),
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMPTZ  NOT NULL,
    summary    JSONB
);

CREATE INDEX idx_option_groups_product ON product_option_groups (product_id);
CREATE INDEX idx_option_values_group ON product_option_values (option_group_id);
CREATE INDEX idx_variants_product ON product_variants (product_id);
CREATE INDEX idx_images_product ON product_images (product_id);
CREATE INDEX idx_change_logs_product ON product_change_logs (product_id);
