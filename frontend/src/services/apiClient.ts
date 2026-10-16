const BASE_PATH = '/api/v1';

export interface ApiFieldError {
  field: string;
  message: string;
}

interface ApiErrorInit {
  status: number;
  title: string;
  detail: string;
  requestId?: string;
  errors?: ApiFieldError[];
}

/** 서버의 Problem Details(RFC 9457) 오류 응답을 담는 예외. */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly requestId?: string;
  readonly errors: ApiFieldError[];

  constructor(init: ApiErrorInit) {
    super(init.detail);
    this.name = 'ApiError';
    this.status = init.status;
    this.title = init.title;
    this.detail = init.detail;
    this.requestId = init.requestId;
    this.errors = init.errors ?? [];
  }
}

type QueryValue = string | number | undefined | null;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  params?: Record<string, QueryValue>;
  /** JSON으로 직렬화해 보낸다. */
  body?: unknown;
  /** multipart 업로드용. 지정하면 `body`보다 우선한다. */
  formData?: FormData;
}

/** 개발·테스트 프로필용 HTTP Basic 헤더. 환경 변수가 없으면 null이다. */
function basicAuthHeader(): string | null {
  const username = import.meta.env.VITE_DEV_USERNAME;
  const password = import.meta.env.VITE_DEV_PASSWORD;
  if (!username || !password) return null;
  return `Basic ${btoa(`${username}:${password}`)}`;
}

function buildUrl(path: string, params?: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value));
  }
  const query = search.toString();
  return `${BASE_PATH}${path}${query ? `?${query}` : ''}`;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = await response.json();
    return new ApiError({
      status: problem.status ?? response.status,
      title: problem.title ?? '요청 실패',
      detail: problem.detail ?? '요청을 처리하지 못했습니다.',
      requestId: problem.requestId,
      errors: problem.errors,
    });
  } catch {
    return new ApiError({
      status: response.status,
      title: '요청 실패',
      detail: '서버 응답을 해석하지 못했습니다. 잠시 후 다시 시도하세요.',
    });
  }
}

/** `/api/v1` 아래 엔드포인트를 호출한다. 실패하면 {@link ApiError}를 던진다. */
export async function apiRequest<T = unknown>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers();
  const authorization = basicAuthHeader();
  if (authorization) headers.set('Authorization', authorization);

  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData; // Content-Type(boundary 포함)은 브라우저가 지정한다.
  } else if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(options.body);
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, options.params), {
      method: options.method ?? 'GET',
      headers,
      body,
    });
  } catch {
    throw new ApiError({
      status: 0,
      title: '네트워크 오류',
      detail: '서버에 연결하지 못했습니다. 네트워크 상태를 확인하고 다시 시도하세요.',
    });
  }

  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
