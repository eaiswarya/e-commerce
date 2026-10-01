import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/client';
import { returnLoan } from '../../api/loans';
import { renderWithProviders } from '../../test/render';
import { LOAN } from '../../test/fixtures';
import type { Loan } from '../../types';
import { formatDate } from '../../utils/dates';
import { LoansTable } from './LoansTable';

vi.mock('../../api/loans', () => ({ returnLoan: vi.fn() }));

const RETURNED: Loan = { ...LOAN, id: 12, status: 'RETURNED', returnedAt: '2026-09-20T12:00:00Z' };

beforeEach(() => {
  vi.mocked(returnLoan).mockReset();
});

describe('LoansTable', () => {
  it('shows each loan with links, due date and status', () => {
    renderWithProviders(<LoansTable caption="Loans" loans={[LOAN]} />);

    const table = screen.getByRole('table', { name: 'Loans' });
    expect(within(table).getByRole('link', { name: 'Dune' })).toHaveAttribute('href', '/books/3');
    expect(within(table).getByRole('link', { name: 'Ada Lovelace (M0007)' })).toHaveAttribute(
      'href',
      '/members/7',
    );
    expect(within(table).getByText(formatDate('2026-09-29'))).toBeInTheDocument();
    expect(within(table).getByText('Overdue')).toBeInTheDocument();
  });

  it('offers Return only for loans not yet returned', () => {
    renderWithProviders(<LoansTable caption="Loans" loans={[LOAN, RETURNED]} />);

    expect(screen.getAllByRole('button', { name: /^Return / })).toHaveLength(1);
    expect(screen.getByText('Returned')).toBeInTheDocument();
  });

  it('can leave out the member column on a member page', () => {
    renderWithProviders(<LoansTable caption="Loans" loans={[LOAN]} hide="member" />);

    expect(screen.queryByRole('columnheader', { name: 'Member' })).not.toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Book' })).toBeInTheDocument();
  });

  it('returns a loan', async () => {
    vi.mocked(returnLoan).mockResolvedValue(RETURNED);
    renderWithProviders(<LoansTable caption="Loans" loans={[LOAN]} />);

    await userEvent.click(screen.getByRole('button', { name: 'Return Dune from Ada Lovelace' }));

    expect(returnLoan).toHaveBeenCalledWith(11);
    expect(await screen.findByText('“Dune” returned')).toBeInTheDocument();
  });

  it('explains a failed return', async () => {
    vi.mocked(returnLoan).mockRejectedValue(
      new ApiError(409, 'ALREADY_RETURNED', 'Loan 11 was already returned'),
    );
    renderWithProviders(<LoansTable caption="Loans" loans={[LOAN]} />);

    await userEvent.click(screen.getByRole('button', { name: 'Return Dune from Ada Lovelace' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('This loan has already been returned.');
  });
});
