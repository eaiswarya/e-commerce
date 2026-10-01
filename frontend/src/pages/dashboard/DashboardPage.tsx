import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import {
  ArrowLeftRight,
  ArrowRight,
  BookOpen,
  BookUp,
  CalendarClock,
  CircleCheck,
  Users,
  type LucideIcon,
} from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { searchBooks } from '../../api/books';
import { searchLoans, type LoanSearch } from '../../api/loans';
import { searchMembers } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import { useAuth } from '../../auth/authContext';
import { EmptyState } from '../../components/EmptyState';
import { BorrowDialog } from '../../components/loans/BorrowDialog';
import { LoansTable } from '../../components/loans/LoansTable';
import { QueryState } from '../../components/QueryState';
import type { PageResponse } from '../../types';
import styles from './DashboardPage.module.css';

/** Counts come from the list endpoints: asking for one row still returns the total. */
const COUNT = { size: 1 } as const;
const OVERDUE: LoanSearch = { status: 'overdue', sort: 'dueDate,asc', size: 10 };

export function DashboardPage() {
  const { user } = useAuth();
  const [lending, setLending] = useState(false);
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
        <div>
          <h1>Dashboard</h1>
          <p className="page-subtitle">Welcome back, {user?.fullName}. Here is the desk at a glance.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setLending(true)}>
          <BookUp />
          Lend a book
        </button>
      </div>
      <ul className={styles.tiles}>
        <StatTile label="Books" to="/books" icon={BookOpen} query={books} />
        <StatTile label="Active members" to="/members?status=active" icon={Users} query={members} />
        <StatTile label="Books on loan" to="/loans" icon={ArrowLeftRight} query={onLoan} />
        <StatTile label="Overdue" to="/loans?status=overdue" icon={CalendarClock} query={overdue} alert />
      </ul>
      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Overdue loans</h2>
          <Link to="/loans?status=overdue" className={styles.more}>
            See all overdue <ArrowRight />
          </Link>
        </div>
        <QueryState
          query={overdue}
          isEmpty={(page) => page.content.length === 0}
          empty={<EmptyState icon={CircleCheck} message="Nothing is overdue." />}
        >
          {(page) => <LoansTable caption="Overdue loans" loans={page.content} />}
        </QueryState>
      </div>
      <BorrowDialog open={lending} onOpenChange={setLending} />
    </section>
  );
}

interface StatTileProps {
  label: string;
  to: string;
  icon: LucideIcon;
  query: UseQueryResult<PageResponse<unknown>>;
  /** Highlights a non-zero count that needs attention. */
  alert?: boolean;
}

/** One total; a failure here shows in the tile only, so the rest of the dashboard still works. */
function StatTile({ label, to, icon: Icon, query, alert = false }: StatTileProps) {
  const count = query.data?.totalElements;
  return (
    <li className={`${styles.tile} ${alert && count ? styles.alert : ''}`}>
      <div className={styles.tileHeader}>
        <Link to={to} className={styles.label}>
          {label}
        </Link>
        <span className={styles.icon}>
          <Icon />
        </span>
      </div>
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
            className="btn btn-sm"
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
