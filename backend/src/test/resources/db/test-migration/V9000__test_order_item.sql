-- 테스트 전용: 상품을 참조하는 주문 항목 테이블. 주문 기능은 별도 기능이므로 운영 스키마에는 없다.
-- 주문 이력이 있는 상품을 삭제해도 과거 주문 정보가 보존되는지(SC-006) 검증하는 데 쓴다.
CREATE TABLE test_order_item (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id   BIGINT       NOT NULL REFERENCES products (id),
    product_name VARCHAR(200) NOT NULL,
    unit_price   BIGINT       NOT NULL
);
