import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { memberLoans, type LoanFilter } from '../../api/loans';
import { queryKeys } from '../../api/queryKeys';
import { EmptyState } from '../../components/EmptyState';
import { LoansTable } from '../../components/loans/LoansTable';
import { Pagination } from '../../components/Pagination';
import { QueryState } from '../../components/QueryState';

function useMemberLoans(memberId: number, status: LoanFilter, page: number) {
  return useQuery({
    queryKey: queryKeys.memberLoans(memberId, status, page),
    queryFn: ({ signal }) => memberLoans(memberId, { status, page }, signal),
    placeholderData: keepPreviousData,
  });
}

/** What the member has now (with Return) and what they returned before. */
export function MemberLoans({ memberId }: { memberId: number }) {
  const [historyPage, setHistoryPage] = useState(0);
  const current = useMemberLoans(memberId, 'active', 0);
  const history = useMemberLoans(memberId, 'returned', historyPage);

  return (
    <>
      <div className="section">
        <div className="section-header">
          <h2 className="section-title">On loan</h2>
        </div>
        <QueryState
          query={current}
          isEmpty={(page) => page.content.length === 0}
          empty={<EmptyState message="Nothing on loan." />}
        >
          {(page) => <LoansTable caption="On loan" loans={page.content} hide="member" />}
        </QueryState>
      </div>
      <div className="section">
        <div className="section-header">
          <h2 className="section-title">History</h2>
        </div>
        <QueryState
          query={history}
          isEmpty={(page) => page.content.length === 0}
          empty={<EmptyState message="No returned books yet." />}
        >
          {(page) => (
            <>
              <LoansTable caption="History" loans={page.content} hide="member" />
              <Pagination page={page.page} totalPages={page.totalPages} onChange={setHistoryPage} />
            </>
          )}
        </QueryState>
      </div>
    </>
  );
}
