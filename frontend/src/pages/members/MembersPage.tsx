import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { searchMembers, type MemberSearch } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import { Badge } from '../../components/Badge';
import { DataTable, type Column } from '../../components/DataTable';
import { EmptyState } from '../../components/EmptyState';
import { Pagination } from '../../components/Pagination';
import { QueryState } from '../../components/QueryState';
import { SearchInput } from '../../components/SearchInput';
import type { Member } from '../../types';
import { useListParams } from '../useListParams';
import { MemberFormDialog } from './MemberFormDialog';

const columns: Column<Member>[] = [
  { header: 'Code', cell: (member) => member.memberCode },
  { header: 'Name', cell: (member) => <Link to={`/members/${member.id}`}>{member.fullName}</Link> },
  { header: 'Email', cell: (member) => member.email },
  { header: 'Phone', cell: (member) => member.phone ?? '—' },
  {
    header: 'Status',
    cell: (member) =>
      member.active ? <Badge tone="success">Active</Badge> : <Badge tone="muted">Inactive</Badge>,
  },
];

/** URL value of the status filter → the API's {@code active} parameter (omitted = everyone). */
const STATUS: Record<string, boolean | undefined> = { active: true, inactive: false };

export function MembersPage() {
  const { params, page: pageNumber, update, setPage } = useListParams();
  const status = params.get('status') ?? 'all';
  const search: MemberSearch = {
    q: params.get('q') ?? '',
    active: STATUS[status],
    page: pageNumber,
  };
  const [adding, setAdding] = useState(false);
  const query = useQuery({
    queryKey: queryKeys.memberSearch(search),
    queryFn: ({ signal }) => searchMembers(search, signal),
    placeholderData: keepPreviousData,
  });

  const filtered = Boolean(search.q) || status !== 'all';

  return (
    <section>
      <div className="page-header">
        <h1>Members</h1>
        <button type="button" className="btn btn-primary" onClick={() => setAdding(true)}>
          Add member
        </button>
      </div>
      <div className="toolbar">
        <SearchInput
          label="Search"
          placeholder="Name, email or member code"
          value={search.q ?? ''}
          onSearch={(q) => update({ q })}
        />
        <label>
          Status{' '}
          <select
            value={status}
            onChange={(event) => update({ status: event.target.value === 'all' ? null : event.target.value })}
          >
            <option value="all">All</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </label>
      </div>
      <QueryState
        query={query}
        isEmpty={(page) => page.content.length === 0}
        empty={
          <EmptyState
            message={filtered ? 'No members match your search.' : 'No members yet. Add the first one.'}
          />
        }
      >
        {(page) => (
          <>
            <DataTable
              caption="Members"
              columns={columns}
              rows={page.content}
              rowKey={(member) => member.id}
            />
            <Pagination page={page.page} totalPages={page.totalPages} onChange={setPage} />
          </>
        )}
      </QueryState>
      <MemberFormDialog open={adding} onOpenChange={setAdding} />
    </section>
  );
}
