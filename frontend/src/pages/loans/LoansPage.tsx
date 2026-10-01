import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { BookUp } from 'lucide-react';
import { useState } from 'react';
import { searchLoans, type LoanFilter, type LoanSearch } from '../../api/loans';
import { queryKeys } from '../../api/queryKeys';
import { EmptyState } from '../../components/EmptyState';
import { BorrowDialog } from '../../components/loans/BorrowDialog';
import { LoansTable } from '../../components/loans/LoansTable';
import { Pagination } from '../../components/Pagination';
import { QueryState } from '../../components/QueryState';
import { useListParams } from '../useListParams';

const FILTERS: { value: LoanFilter | 'all'; label: string; empty: string }[] = [
  { value: 'active', label: 'On loan', empty: 'No books are on loan.' },
  { value: 'overdue', label: 'Overdue', empty: 'Nothing is overdue.' },
  { value: 'returned', label: 'Returned', empty: 'No books have been returned yet.' },
  { value: 'all', label: 'All', empty: 'No loans yet.' },
];

export function LoansPage() {
  const { params, page, update, setPage } = useListParams();
  // The desk mostly needs what is out right now, so that is the default view.
  const filter = FILTERS.find((f) => f.value === params.get('status')) ?? FILTERS[0];
  const search: LoanSearch = { status: filter.value === 'all' ? undefined : filter.value, page };
  const [lending, setLending] = useState(false);
  const query = useQuery({
    queryKey: queryKeys.loanSearch(search),
    queryFn: ({ signal }) => searchLoans(search, signal),
    placeholderData: keepPreviousData,
  });

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Loans</h1>
          <p className="page-subtitle">What is out, what is late, and what came back.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setLending(true)}>
          <BookUp />
          Lend a book
        </button>
      </div>
      <div className="toolbar">
        <div className="segmented" role="group" aria-label="Show loans">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              aria-pressed={f === filter}
              onClick={() => update({ status: f.value === 'active' ? null : f.value })}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>
      <QueryState
        query={query}
        isEmpty={(result) => result.content.length === 0}
        empty={<EmptyState message={filter.empty} />}
      >
        {(result) => (
          <>
            <LoansTable caption={`Loans: ${filter.label}`} loans={result.content} />
            <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} />
          </>
        )}
      </QueryState>
      <BorrowDialog open={lending} onOpenChange={setLending} />
    </section>
  );
}
