import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { deleteBook, getBook } from '../../api/books';
import { searchLoans } from '../../api/loans';
import { LOAN, pageOf } from '../../test/fixtures';
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

vi.mock('../../api/loans', () => ({ searchLoans: vi.fn(), returnLoan: vi.fn(), borrowBook: vi.fn() }));
vi.mock('../../api/members', () => ({ searchMembers: vi.fn().mockResolvedValue({ content: [] }) }));

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
  vi.mocked(searchLoans).mockReset().mockResolvedValue(pageOf([]));
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
    expect(await screen.findByText('“Effective Java” deleted')).toBeInTheDocument();
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

  it('shows who has a copy now', async () => {
    vi.mocked(getBook).mockResolvedValue({ ...BOOK, availableCopies: 2 });
    vi.mocked(searchLoans).mockResolvedValue(pageOf([{ ...LOAN, bookId: 7, status: 'ACTIVE' }]));
    renderPage();

    const table = await screen.findByRole('table', { name: 'On loan now' });
    expect(table).toHaveTextContent('Ada Lovelace (M0007)');
    expect(searchLoans).toHaveBeenCalledWith({ bookId: 7, status: 'active', page: 0 }, expect.anything());
  });

  it('says when no copies are out', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    renderPage();

    expect(await screen.findByText('No copies are on loan.')).toBeInTheDocument();
  });

  it('lends this book from its page', async () => {
    vi.mocked(getBook).mockResolvedValue(BOOK);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Lend this book' }));

    expect(screen.getByRole('dialog', { name: 'Lend a book' })).toHaveTextContent('Book: Effective Java');
  });

  it('does not offer lending when every copy is out', async () => {
    vi.mocked(getBook).mockResolvedValue({ ...BOOK, availableCopies: 0 });
    renderPage();

    await screen.findByRole('heading', { name: 'Effective Java' });
    expect(screen.queryByRole('button', { name: 'Lend this book' })).not.toBeInTheDocument();
  });
});
