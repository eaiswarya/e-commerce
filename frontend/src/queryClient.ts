import { QueryClient } from '@tanstack/react-query';
import { ApiError } from './api/client';

/** Retries a failed query once, except for client errors (4xx), which a retry would not fix. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          failureCount < 1 && !(error instanceof ApiError && error.status >= 400 && error.status < 500),
        refetchOnWindowFocus: false,
      },
    },
  });
}
