-- 개발 프로필 전용 샘플 카테고리. 반복 실행 마이그레이션이므로 버전 순서와 무관하게 적용된다.
INSERT INTO categories (name) VALUES ('의류'), ('신발'), ('가방'), ('생활용품') ON CONFLICT (name) DO NOTHING;
