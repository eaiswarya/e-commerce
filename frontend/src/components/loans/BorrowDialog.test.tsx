import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { searchBooks } from '../../api/books';
import { ApiError } from '../../api/client';
import { borrowBook } from '../../api/loans';
import { searchMembers } from '../../api/members';
import { BOOK, LOAN, MEMBER, pageOf } from '../../test/fixtures';
import { renderWithProviders } from '../../test/render';
import { formatDate } from '../../utils/dates';
import { BorrowDialog } from './BorrowDialog';

vi.mock('../../api/books', () => ({ searchBooks: vi.fn() }));
vi.mock('../../api/members', () => ({ searchMembers: vi.fn() }));
vi.mock('../../api/loans', () => ({ borrowBook: vi.fn() }));

function renderDialog(props: Partial<Parameters<typeof BorrowDialog>[0]> = {}) {
  const onOpenChange = vi.fn();
  renderWithProviders(<BorrowDialog open onOpenChange={onOpenChange} {...props} />);
  return { onOpenChange };
}

async function chooseBoth() {
  await userEvent.click(await screen.findByRole('radio', { name: /Ada Lovelace \(M0007\)/ }));
  await userEvent.click(await screen.findByRole('radio', { name: /Dune by Frank Herbert/ }));
}

const lend = () => userEvent.click(screen.getByRole('button', { name: 'Lend book' }));

beforeEach(() => {
  vi.mocked(searchMembers)
    .mockReset()
    .mockResolvedValue(pageOf([MEMBER]));
  vi.mocked(searchBooks)
    .mockReset()
    .mockResolvedValue(pageOf([BOOK]));
  vi.mocked(borrowBook).mockReset();
});

describe('BorrowDialog', () => {
  it('offers only active members and books with a free copy', async () => {
    renderDialog();

    expect(await screen.findByRole('radio', { name: /Ada Lovelace/ })).toBeInTheDocument();
    expect(screen.getByText('1 of 2 available')).toBeInTheDocument();
    expect(searchMembers).toHaveBeenCalledWith({ q: '', active: true, size: 10 }, expect.anything());
    expect(searchBooks).toHaveBeenCalledWith({ q: '', available: true, size: 10 }, expect.anything());
  });

  it('searches each list as the librarian types', async () => {
    renderDialog();
    await screen.findByRole('radio', { name: /Ada Lovelace/ });

    await userEvent.type(screen.getByRole('searchbox', { name: 'Find book' }), 'dune');

    await waitFor(() =>
      expect(searchBooks).toHaveBeenLastCalledWith(
        { q: 'dune', available: true, size: 10 },
        expect.anything(),
      ),
    );
  });

  it('can only lend once both a member and a book are chosen', async () => {
    renderDialog();

    expect(screen.getByRole('button', { name: 'Lend book' })).toBeDisabled();
    await userEvent.click(await screen.findByRole('radio', { name: /Ada Lovelace/ }));
    expect(screen.getByRole('button', { name: 'Lend book' })).toBeDisabled();
    await userEvent.click(await screen.findByRole('radio', { name: /Dune by/ }));

    expect(screen.getByRole('status')).toHaveTextContent('Lend Dune to Ada Lovelace.');
    expect(screen.getByRole('button', { name: 'Lend book' })).toBeEnabled();
  });

  it('lends the chosen book to the chosen member and closes', async () => {
    vi.mocked(borrowBook).mockResolvedValue({ ...LOAN, status: 'ACTIVE' });
    const { onOpenChange } = renderDialog();

    await chooseBoth();
    await lend();

    expect(borrowBook).toHaveBeenCalledWith(3, 7);
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(await screen.findByText('“Dune” lent to Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText(`Due back ${formatDate(LOAN.dueDate)}`)).toBeInTheDocument();
  });

  it('starts with the member from the member page and asks only for the book', async () => {
    vi.mocked(borrowBook).mockResolvedValue({ ...LOAN, status: 'ACTIVE' });
    renderDialog({ member: MEMBER });

    expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.queryByRole('searchbox', { name: 'Find member' })).not.toBeInTheDocument();
    expect(searchMembers).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole('radio', { name: /Dune by/ }));
    await lend();

    expect(borrowBook).toHaveBeenCalledWith(3, 7);
  });

  it('starts with the book from the book page and asks only for the member', async () => {
    renderDialog({ book: BOOK });

    expect(screen.queryByRole('searchbox', { name: 'Find book' })).not.toBeInTheDocument();
    expect(searchBooks).not.toHaveBeenCalled();
    expect(await screen.findByRole('radio', { name: /Ada Lovelace/ })).toBeInTheDocument();
  });

  it.each([
    ['NO_COPIES_AVAILABLE', 'No copies of this book are available right now.'],
    ['LOAN_LIMIT_REACHED', 'This member already has the maximum number of books on loan.'],
    ['MEMBER_HAS_OVERDUE', 'This member has overdue books and must return them before borrowing more.'],
    ['MEMBER_INACTIVE', 'This member is inactive and cannot borrow books.'],
    ['CONCURRENT_UPDATE', 'Someone else changed this record while you were editing.'],
  ])('explains a refused loan (%s) and stays open', async (code, message) => {
    vi.mocked(borrowBook).mockRejectedValue(new ApiError(409, code, 'server wording'));
    const { onOpenChange } = renderDialog();

    await chooseBoth();
    await lend();

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('says when nothing matches', async () => {
    vi.mocked(searchBooks).mockResolvedValue(pageOf([]));
    renderDialog();

    expect(await screen.findByText('No available books match.')).toBeInTheDocument();
  });
});
