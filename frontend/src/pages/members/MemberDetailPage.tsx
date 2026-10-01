import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { deactivateMember, getMember } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import { Badge } from '../../components/Badge';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { BorrowDialog } from '../../components/loans/BorrowDialog';
import { QueryState } from '../../components/QueryState';
import { formatDateTime } from '../../utils/dates';
import { MemberFormDialog } from './MemberFormDialog';
import { MemberLoans } from './MemberLoans';

export function MemberDetailPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [lending, setLending] = useState(false);
  const query = useQuery({ queryKey: queryKeys.member(id), queryFn: ({ signal }) => getMember(id, signal) });
  const deactivate = useMutation({
    mutationFn: () => deactivateMember(id),
    onSuccess: (member) => {
      queryClient.setQueryData(queryKeys.member(id), member);
      void queryClient.invalidateQueries({ queryKey: queryKeys.memberSearches });
      setDeactivating(false);
    },
  });

  return (
    <section>
      <p>
        <Link to="/members">← All members</Link>
      </p>
      <QueryState
        query={query}
        notFound={
          <EmptyState message="Member not found." action={<Link to="/members">Back to members</Link>} />
        }
      >
        {(member) => (
          <>
            <div className="page-header">
              <h1>
                {member.fullName}{' '}
                {member.active ? <Badge tone="success">Active</Badge> : <Badge tone="muted">Inactive</Badge>}
              </h1>
              <div className="header-actions">
                {member.active && (
                  <button type="button" className="btn btn-primary" onClick={() => setLending(true)}>
                    Lend a book
                  </button>
                )}
                <button type="button" className="btn" onClick={() => setEditing(true)}>
                  Edit
                </button>
                {member.active && (
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => {
                      deactivate.reset();
                      setDeactivating(true);
                    }}
                  >
                    Deactivate
                  </button>
                )}
              </div>
            </div>
            <dl className="details">
              <dt>Member code</dt>
              <dd>{member.memberCode}</dd>
              <dt>Email</dt>
              <dd>{member.email}</dd>
              <dt>Phone</dt>
              <dd>{member.phone ?? '—'}</dd>
              <dt>Joined</dt>
              <dd>{formatDateTime(member.joinedAt)}</dd>
            </dl>
            <MemberLoans memberId={member.id} />
            <MemberFormDialog open={editing} onOpenChange={setEditing} member={member} />
            <BorrowDialog open={lending} onOpenChange={setLending} member={member} />
            <ConfirmDialog
              open={deactivating}
              onOpenChange={setDeactivating}
              title="Deactivate member?"
              confirmLabel="Deactivate"
              onConfirm={() => deactivate.mutate()}
              isPending={deactivate.isPending}
              error={deactivate.error}
            >
              <p>
                {member.fullName} will no longer be able to borrow books. Their loan history is kept, and
                books they have now can still be returned.
              </p>
            </ConfirmDialog>
          </>
        )}
      </QueryState>
    </section>
  );
}
