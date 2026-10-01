import type { Loan, PageResponse } from '../types';
import { apiFetch, toQuery } from './client';

/** {@code active} includes overdue loans, as in the API; omitted means every loan. */
export type LoanFilter = 'active' | 'overdue' | 'returned';

export interface LoanSearch {
  status?: LoanFilter;
  memberId?: number;
  bookId?: number;
  page?: number;
  size?: number;
  /** e.g. {@code dueDate,asc}; the API defaults to newest first. */
  sort?: string;
}

export function searchLoans(
  { status, memberId, bookId, page, size, sort }: LoanSearch,
  signal?: AbortSignal,
) {
  const query = toQuery({ status, memberId, bookId, page: page || undefined, size, sort });
  return apiFetch<PageResponse<Loan>>(`/api/loans${query}`, { signal });
}

export function memberLoans(
  memberId: number,
  { status, page }: { status?: LoanFilter; page?: number },
  signal?: AbortSignal,
) {
  const query = toQuery({ status, page: page || undefined });
  return apiFetch<PageResponse<Loan>>(`/api/members/${memberId}/loans${query}`, { signal });
}

export function borrowBook(bookId: number, memberId: number) {
  return apiFetch<Loan>('/api/loans', { method: 'POST', body: { bookId, memberId } });
}

export function returnLoan(id: number) {
  return apiFetch<Loan>(`/api/loans/${id}/return`, { method: 'POST' });
}
