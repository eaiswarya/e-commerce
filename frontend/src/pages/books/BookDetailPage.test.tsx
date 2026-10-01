import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { deleteBook, getBook } from '../../api/books';
import { ApiError } from '../../api/client';
import { renderWithProviders } from '../../test/render';
import type { Book } from '../../types';
import { BookDetailPage } from './BookDetailPage';

vi.mock('../../api/books', () => ({
  getBook: vi.fn(),
  deleteBook: vi.fn(),
  updateBook: vi.fn(),
  createBook: vi.fn(),
}));

const BOOK: Book = {
  id: 7,
  isbn: '9780134685991',
  title: 'Effective Java',
  author: 'Joshua Bloch',
  category: null,
  publishedYear: 2018,
  totalCopies: 3,
  availableCopies: 3,
  version: 4,
};

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/books" element={<p>Books list</p>} />
      <Route path="/books/:id" element={<BookDetailPage />} />
    </Routes>,
    { route: '/books/7' },
  );
}

beforeEach(() => {
  vi.mocked(getBook).mockReset();
  vi.mocked(deleteBook).mockReset();
});

describe('BookDetailPage', () => {
  it('shows the book', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Effective Java' })).toBeInTheDocument();
    expect(screen.getByText('Joshua Bloch')).toBeInTheDocument();
    expect(screen.getByText('3 of 3')).toBeInTheDocument();
    expect(getBook).toHaveBeenCalledWith(7, expect.anything());
  });

  it('says so when the book does not exist', async () => {
    vi.mocked(getBook).mockRejectedValue(new ApiError(404, 'NOT_FOUND', 'Book 7 not found'));
    renderPage();

    expect(await screen.findByText('Book not found. It may have been deleted.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to books' })).toBeInTheDocument();
  });

  it('opens the edit form with the book', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }));

    expect(screen.getByRole('dialog', { name: 'Edit book' })).toBeInTheDocument();
    expect(screen.getByLabelText('ISBN')).toHaveValue('9780134685991');
  });

  it('deletes after confirmation and returns to the list', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    vi.mocked(deleteBook).mockResolvedValue(undefined);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    expect(screen.getByRole('dialog', { name: 'Delete book?' })).toHaveTextContent(
      '“Effective Java” will be removed',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByText('Books list')).toBeInTheDocument();
    expect(deleteBook).toHaveBeenCalledWith(7);
  });

  it('explains why a borrowed book cannot be deleted', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    vi.mocked(deleteBook).mockRejectedValue(
      new ApiError(409, 'HAS_LOAN_HISTORY', 'Book 7 has been borrowed before'),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Set its total copies to 0 to withdraw it.');
    expect(screen.getByRole('dialog', { name: 'Delete book?' })).toBeInTheDocument();
  });
});
