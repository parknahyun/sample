import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// 개발 서버는 /api 요청을 백엔드로 전달한다(같은 출처이므로 CORS 설정이 필요 없다).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': process.env.API_PROXY_TARGET || 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    include: ['tests/unit/**/*.test.{ts,tsx}'],
    setupFiles: ['tests/unit/setup.ts'],
  },
});
