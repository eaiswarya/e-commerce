import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderResult } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, type InitialEntry } from 'react-router';
import { AuthProvider } from '../auth/AuthProvider';

/** Renders {@code ui} inside the app's providers, starting at {@code route} (a path, or a path with state). */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/' }: { route?: InitialEntry } = {},
): RenderResult {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>{ui}</AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
