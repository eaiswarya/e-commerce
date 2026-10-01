import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { QueryState } from './QueryState';

function Harness({ load }: { load: () => Promise<string[]> }) {
  const query = useQuery({ queryKey: ['items'], queryFn: load });
  return (
    <QueryState
      query={query}
      isEmpty={(items) => items.length === 0}
      empty={<p>No items</p>}
      notFound={<p>Gone</p>}
    >
      {(items) => <p>Items: {items.join(', ')}</p>}
    </QueryState>
  );
}

function renderHarness(load: () => Promise<string[]>) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <Harness load={load} />
    </QueryClientProvider>,
  );
}

describe('QueryState', () => {
  it('shows loading while the data is on its way', () => {
    renderHarness(() => new Promise(() => {}));

    expect(screen.getByRole('status')).toHaveTextContent('Loading…');
  });

  it('shows the data once loaded', async () => {
    renderHarness(() => Promise.resolve(['a', 'b']));

    expect(await screen.findByText('Items: a, b')).toBeInTheDocument();
  });

  it('shows the empty state when there is nothing', async () => {
    renderHarness(() => Promise.resolve([]));

    expect(await screen.findByText('No items')).toBeInTheDocument();
  });

  it('shows the error with a Retry that loads again', async () => {
    const load = vi
      .fn<() => Promise<string[]>>()
      .mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.'))
      .mockResolvedValueOnce(['back']);
    renderHarness(load);

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server.');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByText('Items: back')).toBeInTheDocument();
  });

  it('shows the not-found content for a 404', async () => {
    renderHarness(() => Promise.reject(new ApiError(404, 'NOT_FOUND', 'Book 7 not found')));

    expect(await screen.findByText('Gone')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
