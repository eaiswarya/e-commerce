import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { Link } from 'react-router';
import { searchBooks } from '../../api/books';
import { searchLoans, type LoanSearch } from '../../api/loans';
import { searchMembers } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import { useAuth } from '../../auth/authContext';
import { EmptyState } from '../../components/EmptyState';
import { LoansTable } from '../../components/loans/LoansTable';
import { QueryState } from '../../components/QueryState';
import type { PageResponse } from '../../types';
import styles from './DashboardPage.module.css';

/** Counts come from the list endpoints: asking for one row still returns the total. */
const COUNT = { size: 1 } as const;
const OVERDUE: LoanSearch = { status: 'overdue', sort: 'dueDate,asc', size: 10 };

export function DashboardPage() {
  const { user } = useAuth();
  const books = useQuery({
    queryKey: queryKeys.bookSearch(COUNT),
    queryFn: ({ signal }) => searchBooks(COUNT, signal),
  });
  const members = useQuery({
    queryKey: queryKeys.memberSearch({ active: true, ...COUNT }),
    queryFn: ({ signal }) => searchMembers({ active: true, ...COUNT }, signal),
  });
  const onLoan = useQuery({
    queryKey: queryKeys.loanSearch({ status: 'active', ...COUNT }),
    queryFn: ({ signal }) => searchLoans({ status: 'active', ...COUNT }, signal),
  });
  // The overdue list doubles as the overdue count.
  const overdue = useQuery({
    queryKey: queryKeys.loanSearch(OVERDUE),
    queryFn: ({ signal }) => searchLoans(OVERDUE, signal),
  });

  return (
    <section>
      <div className="page-header">
        <h1>Dashboard</h1>
        <span className={styles.greeting}>Signed in as {user?.fullName}</span>
      </div>
      <ul className={styles.tiles}>
        <StatTile label="Books" to="/books" query={books} />
        <StatTile label="Active members" to="/members?status=active" query={members} />
        <StatTile label="Books on loan" to="/loans" query={onLoan} />
        <StatTile label="Overdue" to="/loans?status=overdue" query={overdue} alert />
      </ul>
      <div className="page-header">
        <h2 className="section-title">Overdue loans</h2>
        <Link to="/loans?status=overdue">See all overdue</Link>
      </div>
      <QueryState
        query={overdue}
        isEmpty={(page) => page.content.length === 0}
        empty={<EmptyState message="Nothing is overdue." />}
      >
        {(page) => <LoansTable caption="Overdue loans" loans={page.content} />}
      </QueryState>
    </section>
  );
}

interface StatTileProps {
  label: string;
  to: string;
  query: UseQueryResult<PageResponse<unknown>>;
  /** Highlights a non-zero count that needs attention. */
  alert?: boolean;
}

/** One total; a failure here shows in the tile only, so the rest of the dashboard still works. */
function StatTile({ label, to, query, alert = false }: StatTileProps) {
  const count = query.data?.totalElements;
  return (
    <li className={`${styles.tile} ${alert && count ? styles.alert : ''}`}>
      <Link to={to} className={styles.label}>
        {label}
      </Link>
      {query.isPending && (
        <span className={styles.value} aria-label={`${label}: loading`}>
          …
        </span>
      )}
      {query.isError && (
        <span className={styles.error}>
          Unavailable{' '}
          <button
            type="button"
            className="btn"
            onClick={() => void query.refetch()}
            aria-label={`Retry ${label}`}
          >
            Retry
          </button>
        </span>
      )}
      {count !== undefined && (
        <span className={styles.value} aria-label={`${label}: ${count}`}>
          {count}
        </span>
      )}
    </li>
  );
}
