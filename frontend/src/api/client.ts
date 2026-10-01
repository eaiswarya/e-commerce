import type { ErrorResponse } from '../types';

/** A failed API call. {@code code} is the API's {@code error} value, e.g. {@code NO_COPIES_AVAILABLE}. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
}

let authToken: string | null = null;
const unauthorizedHandlers = new Set<() => void>();

export function setAuthToken(token: string | null): void {
  authToken = token;
}

/**
 * Registers a handler for a 401 on a request that carried a token, meaning the session has expired or is invalid.
 * Returns a function that removes the handler.
 */
export function onUnauthorized(handler: () => void): () => void {
  unauthorizedHandlers.add(handler);
  return () => unauthorizedHandlers.delete(handler);
}

/** The only place the app calls {@code fetch}. Throws {@link ApiError} for any non-2xx response. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal } = options;
  const token = authToken;
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.');
  }

  if (!response.ok) {
    const error = await toApiError(response);
    // Without a token a 401 is a failed login, which the login form shows; with one, the session is over.
    if (response.status === 401 && token) {
      unauthorizedHandlers.forEach((handler) => handler());
    }
    throw error;
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as Partial<ErrorResponse>;
    if (typeof body.error === 'string' && typeof body.message === 'string') {
      return new ApiError(response.status, body.error, body.message, body.fieldErrors ?? {});
    }
  } catch {
    // Not JSON (e.g. a proxy error page): fall through to the HTTP status.
  }
  return new ApiError(response.status, `HTTP_${response.status}`, response.statusText || 'Request failed');
}

export type QueryValue = string | number | boolean | null | undefined;

/** Builds {@code ?a=1&b=x}, leaving out null, undefined and blank values; returns '' when nothing is left. */
export function toQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || (typeof value === 'string' && value.trim() === '')) {
      continue;
    }
    search.set(key, String(typeof value === 'string' ? value.trim() : value));
  }
  const query = search.toString();
  return query ? `?${query}` : '';
}
