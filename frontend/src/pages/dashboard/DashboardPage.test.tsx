import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { searchBooks } from '../../api/books';
import { ApiError } from '../../api/client';
import { searchLoans } from '../../api/loans';
import { searchMembers } from '../../api/members';
import { saveSession } from '../../auth/session';
import { LOAN, pageOf } from '../../test/fixtures';
import { renderWithProviders } from '../../test/render';
import { DashboardPage } from './DashboardPage';

vi.mock('../../api/books', () => ({ searchBooks: vi.fn() }));
vi.mock('../../api/members', () => ({ searchMembers: vi.fn() }));
vi.mock('../../api/loans', () => ({ searchLoans: vi.fn(), returnLoan: vi.fn() }));

const total = (totalElements: number) => pageOf([], { totalElements });

beforeEach(() => {
  saveSession({
    token: 't',
    expiresAt: new Date(Date.now() + 60_000).toISOString(),
    username: 'admin',
    fullName: 'Administrator',
  });
  vi.mocked(searchBooks).mockReset().mockResolvedValue(total(120));
  vi.mocked(searchMembers).mockReset().mockResolvedValue(total(48));
  vi.mocked(searchLoans)
    .mockReset()
    .mockImplementation(({ status }) =>
      Promise.resolve(status === 'overdue' ? pageOf([LOAN], { totalElements: 3 }) : total(17)),
    );
});

describe('DashboardPage', () => {
  it('shows the four totals, each asking for a single row', async () => {
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByLabelText('Books: 120')).toBeInTheDocument();
    expect(await screen.findByLabelText('Active members: 48')).toBeInTheDocument();
    expect(await screen.findByLabelText('Books on loan: 17')).toBeInTheDocument();
    expect(await screen.findByLabelText('Overdue: 3')).toBeInTheDocument();
    expect(searchBooks).toHaveBeenCalledWith({ size: 1 }, expect.anything());
    expect(searchMembers).toHaveBeenCalledWith({ active: true, size: 1 }, expect.anything());
    expect(searchLoans).toHaveBeenCalledWith({ status: 'active', size: 1 }, expect.anything());
  });

  it('lists overdue loans, oldest due first, with Return', async () => {
    renderWithProviders(<DashboardPage />);

    const table = await screen.findByRole('table', { name: 'Overdue loans' });
    expect(table).toHaveTextContent('Dune');
    expect(screen.getByRole('button', { name: 'Return Dune from Ada Lovelace' })).toBeInTheDocument();
    expect(searchLoans).toHaveBeenCalledWith(
      { status: 'overdue', sort: 'dueDate,asc', size: 10 },
      expect.anything(),
    );
    expect(screen.getByRole('link', { name: 'See all overdue' })).toHaveAttribute(
      'href',
      '/loans?status=overdue',
    );
  });

  it('says when nothing is overdue', async () => {
    vi.mocked(searchLoans).mockResolvedValue(total(0));
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByText('Nothing is overdue.')).toBeInTheDocument();
    expect(await screen.findByLabelText('Overdue: 0')).toBeInTheDocument();
  });

  it('keeps working when one total fails, and that total can be retried', async () => {
    vi.mocked(searchMembers)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'Unexpected error'))
      .mockResolvedValueOnce(total(48));
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByLabelText('Books: 120')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Retry Active members' }));

    expect(await screen.findByLabelText('Active members: 48')).toBeInTheDocument();
  });

  it('links each total to its list', async () => {
    renderWithProviders(<DashboardPage />);

    expect(screen.getByRole('link', { name: 'Active members' })).toHaveAttribute(
      'href',
      '/members?status=active',
    );
    expect(screen.getByRole('link', { name: 'Overdue' })).toHaveAttribute('href', '/loans?status=overdue');
  });
});
