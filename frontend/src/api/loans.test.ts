import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from './client';
import { borrowBook, memberLoans, returnLoan, searchLoans } from './loans';

vi.mock('./client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./client')>()),
  apiFetch: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(apiFetch).mockResolvedValue({});
});

describe('loans api', () => {
  it('searches with only the filters that are set', async () => {
    await searchLoans({ status: 'overdue', size: 10, sort: 'dueDate,asc' });
    await searchLoans({ bookId: 3, status: 'active', page: 2 });
    await searchLoans({});

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/loans?status=overdue&size=10&sort=dueDate%2Casc', {
      signal: undefined,
    });
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/loans?status=active&bookId=3&page=2', {
      signal: undefined,
    });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/api/loans', { signal: undefined });
  });

  it("reads a member's loans", async () => {
    await memberLoans(7, { status: 'returned', page: 1 });

    expect(apiFetch).toHaveBeenCalledWith('/api/members/7/loans?status=returned&page=1', {
      signal: undefined,
    });
  });

  it('lends and takes back a book', async () => {
    await borrowBook(3, 7);
    await returnLoan(11);

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/loans', {
      method: 'POST',
      body: { bookId: 3, memberId: 7 },
    });
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/loans/11/return', { method: 'POST' });
  });
});
