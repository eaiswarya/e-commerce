import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch, onUnauthorized, setAuthToken } from './client';

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  setAuthToken(null);
});

function lastRequest(): { url: string; init: RequestInit; headers: Headers } {
  const [url, init = {}] = fetchMock.mock.calls.at(-1)!;
  return { url: String(url), init, headers: new Headers(init.headers) };
}

describe('apiFetch', () => {
  it('sends JSON with the bearer token and returns the parsed body', async () => {
    setAuthToken('abc.def.ghi');
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 7 }));

    const result = await apiFetch<{ id: number }>('/api/books', { method: 'POST', body: { title: 'Dune' } });

    expect(result).toEqual({ id: 7 });
    const { url, init, headers } = lastRequest();
    expect(url).toBe('/api/books');
    expect(init.method).toBe('POST');
    expect(init.body).toBe('{"title":"Dune"}');
    expect(headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    expect(headers.get('Content-Type')).toBe('application/json');
    expect(headers.get('Accept')).toBe('application/json');
  });

  it('omits the Authorization header and Content-Type when there is no token or body', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []));

    await apiFetch('/api/books');

    const { init, headers } = lastRequest();
    expect(init.method).toBe('GET');
    expect(headers.has('Authorization')).toBe(false);
    expect(headers.has('Content-Type')).toBe(false);
  });

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(apiFetch('/api/books/7', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('turns an error response into an ApiError with code, message and field errors', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        error: 'VALIDATION_FAILED',
        message: 'Request validation failed',
        fieldErrors: { email: 'must be a well-formed email address' },
        timestamp: '2026-09-30T10:00:00Z',
      }),
    );

    const error = await apiFetch('/api/members', { method: 'POST', body: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'Request validation failed',
      fieldErrors: { email: 'must be a well-formed email address' },
    });
  });

  it('falls back to the HTTP status when the error body is not JSON', async () => {
    fetchMock.mockResolvedValue(
      new Response('<html>Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' }),
    );

    await expect(apiFetch('/api/books')).rejects.toMatchObject({
      status: 502,
      code: 'HTTP_502',
      message: 'Bad Gateway',
      fieldErrors: {},
    });
  });

  it('reports a network failure as NETWORK_ERROR', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(apiFetch('/api/books')).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
  });

  it('passes an abort through instead of reporting a network error', async () => {
    fetchMock.mockRejectedValue(new DOMException('The operation was aborted.', 'AbortError'));

    await expect(apiFetch('/api/books')).rejects.toMatchObject({ name: 'AbortError' });
  });

  it('calls the unauthorized handler when a request with a token gets a 401', async () => {
    const handler = vi.fn();
    const unsubscribe = onUnauthorized(handler);
    setAuthToken('expired.token');
    fetchMock.mockResolvedValue(
      jsonResponse(401, { status: 401, error: 'UNAUTHORIZED', message: 'Authentication required' }),
    );

    await expect(apiFetch('/api/books')).rejects.toMatchObject({ status: 401 });

    expect(handler).toHaveBeenCalledOnce();
    unsubscribe();
  });

  it('does not call the unauthorized handler for a 401 without a token, such as a wrong password', async () => {
    const handler = vi.fn();
    const unsubscribe = onUnauthorized(handler);
    fetchMock.mockResolvedValue(
      jsonResponse(401, { status: 401, error: 'UNAUTHORIZED', message: 'Invalid username or password' }),
    );

    await expect(apiFetch('/api/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({
      message: 'Invalid username or password',
    });

    expect(handler).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('stops calling a handler once it unsubscribes', async () => {
    const handler = vi.fn();
    onUnauthorized(handler)();
    setAuthToken('expired.token');
    fetchMock.mockResolvedValue(jsonResponse(401, { status: 401, error: 'UNAUTHORIZED', message: 'x' }));

    await apiFetch('/api/books').catch(() => undefined);

    expect(handler).not.toHaveBeenCalled();
  });
});
