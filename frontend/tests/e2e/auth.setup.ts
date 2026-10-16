import { expect, test as setup } from '@playwright/test';

// 모든 E2E 테스트가 쓰는 개발용 자격 증명(HTTP Basic)이 유효한지 먼저 확인한다.
// 헤더 자체는 playwright.config.ts의 extraHTTPHeaders로 모든 요청에 붙는다.
setup('개발용 자격 증명으로 운영자 API에 인증된다', async ({ request }) => {
  expect(
    process.env.VITE_DEV_USERNAME && process.env.VITE_DEV_PASSWORD,
    'frontend/.env에 VITE_DEV_USERNAME, VITE_DEV_PASSWORD를 설정하세요',
  ).toBeTruthy();

  const response = await request.get('/api/v1/categories');

  expect(response.status(), '자격 증명 또는 백엔드 실행 상태를 확인하세요').toBe(200);
});
