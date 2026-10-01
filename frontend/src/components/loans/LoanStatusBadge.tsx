import type { LoanStatus } from '../../types';
import { Badge } from '../Badge';

const LABELS: Record<LoanStatus, { tone: 'success' | 'muted' | 'danger'; text: string }> = {
  ACTIVE: { tone: 'success', text: 'On loan' },
  OVERDUE: { tone: 'danger', text: 'Overdue' },
  RETURNED: { tone: 'muted', text: 'Returned' },
};

export function LoanStatusBadge({ status }: { status: LoanStatus }) {
  const { tone, text } = LABELS[status];
  return <Badge tone={tone}>{text}</Badge>;
}
