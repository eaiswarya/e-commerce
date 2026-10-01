import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createBook, deleteBook, getBook, searchBooks, updateBook } from './books';
import { apiFetch } from './client';

vi.mock('./client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./client')>()),
  apiFetch: vi.fn(),
}));

const input = {
  isbn: '9780134685991',
  title: 'Effective Java',
  author: 'Joshua Bloch',
  category: null,
  publishedYear: null,
  totalCopies: 2,
};

beforeEach(() => {
  vi.mocked(apiFetch).mockResolvedValue({});
});

describe('books api', () => {
  it('searches with only the filters that are set', async () => {
    await searchBooks({ q: 'java', category: 'Programming', available: true, page: 2 });
    await searchBooks({ q: '', available: false, page: 0 });

    expect(apiFetch).toHaveBeenNthCalledWith(
      1,
      '/api/books?q=java&category=Programming&available=true&page=2',
      {
        signal: undefined,
      },
    );
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/books', { signal: undefined });
  });

  it('reads one book', async () => {
    await getBook(7);

    expect(apiFetch).toHaveBeenCalledWith('/api/books/7', { signal: undefined });
  });

  it('creates, updates and deletes', async () => {
    await createBook(input);
    await updateBook(7, { ...input, version: 3 });
    await deleteBook(7);

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/books', { method: 'POST', body: input });
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/books/7', {
      method: 'PUT',
      body: { ...input, version: 3 },
    });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/api/books/7', { method: 'DELETE' });
  });
});
