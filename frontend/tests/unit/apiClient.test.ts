import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from '../../src/services/apiClient';

function jsonResponse(status: number, body: unknown, contentType = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });
}

function stubFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe('apiRequest', () => {
  it('개발용 자격 증명이 있으면 Basic 인증 헤더를 붙이고 /api/v1 경로로 요청한다', async () => {
    vi.stubEnv('VITE_DEV_USERNAME', 'admin');
    vi.stubEnv('VITE_DEV_PASSWORD', 'secret');
    const fetchMock = stubFetch(jsonResponse(200, [{ id: 1 }]));

    const result = await apiRequest('/categories');

    expect(result).toEqual([{ id: 1 }]);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/categories');
    expect(new Headers(init.headers).get('Authorization')).toBe(`Basic ${btoa('admin:secret')}`);
  });

  it('자격 증명이 없으면 Authorization 헤더를 생략한다', async () => {
    vi.stubEnv('VITE_DEV_USERNAME', '');
    vi.stubEnv('VITE_DEV_PASSWORD', '');
    const fetchMock = stubFetch(jsonResponse(200, {}));

    await apiRequest('/products');

    expect(new Headers(fetchMock.mock.calls[0][1].headers).has('Authorization')).toBe(false);
  });

  it('값이 있는 쿼리 파라미터만 URL에 붙인다', async () => {
    const fetchMock = stubFetch(jsonResponse(200, {}));

    await apiRequest('/products', { params: { q: '셔츠', categoryId: undefined, page: 0 } });

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/products?q=%EC%85%94%EC%B8%A0&page=0');
  });

  it('본문을 JSON으로 보내고 204는 빈 값을 돌려준다', async () => {
    const fetchMock = stubFetch(new Response(null, { status: 204 }));

    const result = await apiRequest('/products/1', { method: 'PUT', body: { name: 'a' } });

    expect(result).toBeUndefined();
    const init = fetchMock.mock.calls[0][1];
    expect(init.method).toBe('PUT');
    expect(init.body).toBe('{"name":"a"}');
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json');
  });

  it('오류 응답의 Problem Details를 ApiError로 던진다', async () => {
    stubFetch(
      jsonResponse(
        400,
        {
          title: '잘못된 요청',
          status: 400,
          detail: '입력값을 확인하세요',
          requestId: 'req-1',
          errors: [{ field: 'name', message: '필수 항목입니다' }],
        },
        'application/problem+json',
      ),
    );

    const error = (await apiRequest('/products', { method: 'POST', body: {} }).catch(
      (e) => e,
    )) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      detail: '입력값을 확인하세요',
      requestId: 'req-1',
      errors: [{ field: 'name', message: '필수 항목입니다' }],
    });
  });

  it('Problem Details가 아닌 오류 응답도 한글 안내가 담긴 ApiError가 된다', async () => {
    stubFetch(new Response('<html></html>', { status: 502 }));

    const error = (await apiRequest('/products').catch((e) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(502);
    expect(error.detail).toMatch(/서버/);
  });

  it('네트워크 오류는 status 0인 ApiError가 된다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('failed')));

    const error = (await apiRequest('/products').catch((e) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
  });
});
