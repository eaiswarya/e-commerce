import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { searchBooks } from '../../api/books';
import { ApiError } from '../../api/client';
import { renderWithProviders } from '../../test/render';
import type { Book, PageResponse } from '../../types';
import { BooksPage } from './BooksPage';

vi.mock('../../api/books', () => ({
  searchBooks: vi.fn(),
  createBook: vi.fn(),
  getBook: vi.fn(),
  updateBook: vi.fn(),
}));

const DUNE: Book = {
  id: 1,
  isbn: '9780441013593',
  title: 'Dune',
  author: 'Frank Herbert',
  category: 'Fiction',
  publishedYear: 1965,
  totalCopies: 3,
  availableCopies: 1,
  version: 0,
};

function page(content: Book[], extra: Partial<PageResponse<Book>> = {}): PageResponse<Book> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    ...extra,
  };
}

function renderPage(route = '/books') {
  return renderWithProviders(
    <Routes>
      <Route path="/books" element={<BooksPage />} />
      <Route path="/books/:id" element={<p>Book detail</p>} />
    </Routes>,
    { route },
  );
}

const lastSearch = () => vi.mocked(searchBooks).mock.calls.at(-1)![0];

beforeEach(() => {
  vi.mocked(searchBooks).mockReset();
});

describe('BooksPage', () => {
  it('shows loading while the books are fetched', () => {
    vi.mocked(searchBooks).mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Loading…');
  });

  it('lists books with their availability and a link to each', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([DUNE]));
    renderPage();

    const table = await screen.findByRole('table', { name: 'Books' });
    expect(within(table).getByText('1 of 3')).toBeInTheDocument();
    await userEvent.click(within(table).getByRole('link', { name: 'Dune' }));
    expect(screen.getByText('Book detail')).toBeInTheDocument();
  });

  it('invites adding the first book when the catalogue is empty', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([]));
    renderPage();

    expect(await screen.findByText('No books yet. Add the first one.')).toBeInTheDocument();
  });

  it('says nothing matched when a search finds no books', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([]));
    renderPage('/books?q=zzz');

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument();
  });

  it('shows a load failure with a working Retry', async () => {
    vi.mocked(searchBooks)
      .mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.'))
      .mockResolvedValueOnce(page([DUNE]));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server.');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('link', { name: 'Dune' })).toBeInTheDocument();
  });

  it('reads its filters from the URL', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([DUNE]));
    renderPage('/books?q=dune&category=Fiction&available=true&page=2');

    await screen.findByRole('table');
    expect(lastSearch()).toEqual({ q: 'dune', category: 'Fiction', available: true, page: 2 });
    expect(screen.getByRole('searchbox', { name: 'Search' })).toHaveValue('dune');
    expect(screen.getByRole('checkbox', { name: 'Available only' })).toBeChecked();
  });

  it('searches as the librarian types and starts again from the first page', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([DUNE]));
    renderPage('/books?page=3');
    await screen.findByRole('table');

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search' }), 'herbert');

    await waitFor(() =>
      expect(lastSearch()).toEqual({ q: 'herbert', category: '', available: false, page: 0 }),
    );
  });

  it('filters to available books', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([DUNE]));
    renderPage();
    await screen.findByRole('table');

    await userEvent.click(screen.getByRole('checkbox', { name: 'Available only' }));

    await waitFor(() => expect(lastSearch()).toMatchObject({ available: true }));
  });

  it('moves between pages', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([DUNE], { totalElements: 45, totalPages: 3 }));
    renderPage();
    await screen.findByRole('table');

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => expect(lastSearch()).toMatchObject({ page: 1 }));
  });

  it('opens the add form', async () => {
    vi.mocked(searchBooks).mockResolvedValue(page([]));
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Add book' }));

    expect(screen.getByRole('dialog', { name: 'Add book' })).toBeInTheDocument();
  });
});
