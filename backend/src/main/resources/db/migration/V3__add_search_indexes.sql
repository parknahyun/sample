-- 상품명 부분 일치 검색(ILIKE)을 위한 trigram 인덱스와 필터·정렬용 인덱스.
-- pg_trgm 확장은 CREATE EXTENSION 권한이 필요하다(운영 DB에서는 DB 관리자가 미리 만들어 둘 수 있다).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);
CREATE INDEX idx_products_status_category ON products (status, category_id);
CREATE INDEX idx_products_created_at ON products (created_at);
CREATE INDEX idx_products_price ON products (price);
