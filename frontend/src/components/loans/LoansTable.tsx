import { Link } from 'react-router';
import { errorMessage } from '../../api/errors';
import type { Loan } from '../../types';
import { formatDate, formatDateTime } from '../../utils/dates';
import { DataTable, type Column } from '../DataTable';
import { ErrorBanner } from '../ErrorBanner';
import { LoanStatusBadge } from './LoanStatusBadge';
import { useReturnLoan } from './useReturnLoan';

interface LoansTableProps {
  caption: string;
  loans: Loan[];
  /** Leave out a column that would repeat the page it is on, e.g. the member on that member's page. */
  hide?: 'book' | 'member';
}

/** Loans with links to their book and member, and a Return button for each loan not yet returned. */
export function LoansTable({ caption, loans, hide }: LoansTableProps) {
  const giveBack = useReturnLoan();

  const columns: Column<Loan>[] = [
    ...(hide === 'book'
      ? []
      : [
          {
            header: 'Book',
            cell: (loan: Loan) => <Link to={`/books/${loan.bookId}`}>{loan.bookTitle}</Link>,
          },
        ]),
    ...(hide === 'member'
      ? []
      : [
          {
            header: 'Member',
            cell: (loan: Loan) => (
              <Link to={`/members/${loan.memberId}`}>
                {loan.memberName} ({loan.memberCode})
              </Link>
            ),
          },
        ]),
    { header: 'Borrowed', cell: (loan) => formatDateTime(loan.borrowedAt) },
    { header: 'Due', cell: (loan) => formatDate(loan.dueDate) },
    {
      header: 'Status',
      cell: (loan) => (
        <>
          <LoanStatusBadge status={loan.status} />
          {loan.returnedAt && <> {formatDateTime(loan.returnedAt)}</>}
        </>
      ),
    },
    {
      header: 'Action',
      cell: (loan) =>
        loan.status === 'RETURNED' ? null : (
          <button
            type="button"
            className="btn"
            aria-label={`Return ${loan.bookTitle} from ${loan.memberName}`}
            disabled={giveBack.isPending && giveBack.variables === loan.id}
            onClick={() => giveBack.mutate(loan.id)}
          >
            {giveBack.isPending && giveBack.variables === loan.id ? 'Returning…' : 'Return'}
          </button>
        ),
    },
  ];

  return (
    <>
      {giveBack.isError && <ErrorBanner message={errorMessage(giveBack.error)} />}
      <DataTable caption={caption} columns={columns} rows={loans} rowKey={(loan) => loan.id} />
    </>
  );
}
