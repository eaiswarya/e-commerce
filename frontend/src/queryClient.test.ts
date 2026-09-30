import { describe, expect, it } from 'vitest';
import { ApiError } from './api/client';
import { createQueryClient } from './queryClient';

const retry = createQueryClient().getDefaultOptions().queries!.retry as (
  count: number,
  error: unknown,
) => boolean;

describe('query retries', () => {
  it('retries a server or network failure once', () => {
    expect(retry(0, new ApiError(503, 'HTTP_503', 'Service Unavailable'))).toBe(true);
    expect(retry(0, new ApiError(0, 'NETWORK_ERROR', 'offline'))).toBe(true);
    expect(retry(1, new ApiError(503, 'HTTP_503', 'Service Unavailable'))).toBe(false);
  });

  it('does not retry client errors', () => {
    expect(retry(0, new ApiError(404, 'NOT_FOUND', 'missing'))).toBe(false);
    expect(retry(0, new ApiError(401, 'UNAUTHORIZED', 'expired'))).toBe(false);
  });
});
