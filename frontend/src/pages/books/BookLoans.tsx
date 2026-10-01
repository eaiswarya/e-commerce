import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { searchLoans, type LoanSearch } from '../../api/loans';
import { queryKeys } from '../../api/queryKeys';
import { EmptyState } from '../../components/EmptyState';
import { LoansTable } from '../../components/loans/LoansTable';
import { Pagination } from '../../components/Pagination';
import { QueryState } from '../../components/QueryState';

/** Who has a copy of this book right now. */
export function BookLoans({ bookId }: { bookId: number }) {
  const [page, setPage] = useState(0);
  const search: LoanSearch = { bookId, status: 'active', page };
  const query = useQuery({
    queryKey: queryKeys.loanSearch(search),
    queryFn: ({ signal }) => searchLoans(search, signal),
  });

  return (
    <>
      <h2 className="section-title">On loan now</h2>
      <QueryState
        query={query}
        isEmpty={(result) => result.content.length === 0}
        empty={<EmptyState message="No copies are on loan." />}
      >
        {(result) => (
          <>
            <LoansTable caption="On loan now" loans={result.content} hide="book" />
            <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} />
          </>
        )}
      </QueryState>
    </>
  );
}
