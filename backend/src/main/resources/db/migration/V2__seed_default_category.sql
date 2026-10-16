-- 카테고리 관리 기능이 제공되기 전에도 상품 등록이 막히지 않도록 기본 카테고리를 둔다.
INSERT INTO categories (name) VALUES ('미분류') ON CONFLICT (name) DO NOTHING;
