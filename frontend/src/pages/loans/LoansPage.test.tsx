import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/client';
import { searchLoans } from '../../api/loans';
import { LOAN, pageOf } from '../../test/fixtures';
import { renderWithProviders } from '../../test/render';
import { LoansPage } from './LoansPage';

vi.mock('../../api/loans', () => ({ searchLoans: vi.fn(), returnLoan: vi.fn(), borrowBook: vi.fn() }));
vi.mock('../../api/books', () => ({ searchBooks: vi.fn().mockResolvedValue({ content: [] }) }));
vi.mock('../../api/members', () => ({ searchMembers: vi.fn().mockResolvedValue({ content: [] }) }));

const lastSearch = () => vi.mocked(searchLoans).mock.calls.at(-1)![0];

beforeEach(() => {
  vi.mocked(searchLoans).mockReset();
});

describe('LoansPage', () => {
  it('shows what is on loan by default', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([LOAN]));
    renderWithProviders(<LoansPage />, { route: '/loans' });

    expect(await screen.findByRole('table', { name: 'Loans: On loan' })).toBeInTheDocument();
    expect(lastSearch()).toEqual({ status: 'active', page: 0 });
    expect(screen.getByRole('button', { name: 'On loan' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('reads the filter and page from the URL', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([LOAN]));
    renderWithProviders(<LoansPage />, { route: '/loans?status=returned&page=2' });

    await screen.findByRole('table');
    expect(lastSearch()).toEqual({ status: 'returned', page: 2 });
  });

  it('switches between overdue and all loans, starting again at page 1', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([LOAN]));
    renderWithProviders(<LoansPage />, { route: '/loans?page=3' });
    await screen.findByRole('table');

    await userEvent.click(screen.getByRole('button', { name: 'Overdue' }));
    await waitFor(() => expect(lastSearch()).toEqual({ status: 'overdue', page: 0 }));

    await userEvent.click(screen.getByRole('button', { name: 'All' }));
    await waitFor(() => expect(lastSearch()).toEqual({ status: undefined, page: 0 }));
  });

  it('says so when nothing is overdue', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([]));
    renderWithProviders(<LoansPage />, { route: '/loans?status=overdue' });

    expect(await screen.findByText('Nothing is overdue.')).toBeInTheDocument();
  });

  it('shows a load failure with Retry', async () => {
    vi.mocked(searchLoans)
      .mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.'))
      .mockResolvedValueOnce(pageOf([LOAN]));
    renderWithProviders(<LoansPage />, { route: '/loans' });

    await userEvent.click(await screen.findByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('table')).toBeInTheDocument();
  });

  it('pages through loans', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([LOAN], { totalPages: 2, totalElements: 30 }));
    renderWithProviders(<LoansPage />, { route: '/loans' });
    await screen.findByRole('table');

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => expect(lastSearch()).toEqual({ status: 'active', page: 1 }));
  });

  it('opens the lending dialog', async () => {
    vi.mocked(searchLoans).mockResolvedValue(pageOf([]));
    renderWithProviders(<LoansPage />, { route: '/loans' });

    await userEvent.click(screen.getByRole('button', { name: 'Lend a book' }));

    expect(screen.getByRole('dialog', { name: 'Lend a book' })).toBeInTheDocument();
  });
});
