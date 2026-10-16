import { defineConfig } from '@playwright/test';

// 개발용 자격 증명은 frontend/.env에서 읽는다(없으면 환경 변수를 그대로 사용).
try {
  process.loadEnvFile('.env');
} catch {
  // .env가 없으면 이미 설정된 환경 변수를 사용한다.
}

const username = process.env.VITE_DEV_USERNAME;
const password = process.env.VITE_DEV_PASSWORD;
const basicAuth =
  username && password
    ? { Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}` }
    : undefined;

/** E2E 설정. 백엔드·DB·프런트엔드 개발 서버가 먼저 실행 중이어야 한다. */
export default defineConfig({
  testDir: 'tests/e2e',
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    extraHTTPHeaders: basicAuth,
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'chromium',
      testIgnore: /auth\.setup\.ts/,
      dependencies: ['setup'],
      use: { browserName: 'chromium' },
    },
  ],
});
