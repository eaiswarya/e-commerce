import type { BookSearch } from './books';
import type { LoanFilter, LoanSearch } from './loans';
import type { MemberSearch } from './members';

/** Every query key in one place, so a mutation can invalidate exactly what it changed. */
export const queryKeys = {
  /** Every book search; invalidate after a write. The detail entry is updated directly instead. */
  bookSearches: ['books', 'search'] as const,
  bookSearch: (search: BookSearch) => ['books', 'search', search] as const,
  book: (id: number) => ['books', 'detail', id] as const,
  memberSearches: ['members', 'search'] as const,
  memberSearch: (search: MemberSearch) => ['members', 'search', search] as const,
  member: (id: number) => ['members', 'detail', id] as const,
  /** Every loan list, including a member's; invalidate (with {@link bookSearches}) after a borrow or return. */
  loans: ['loans'] as const,
  loanSearch: (search: LoanSearch) => ['loans', 'search', search] as const,
  memberLoans: (memberId: number, status: LoanFilter, page: number) =>
    ['loans', 'member', memberId, status, page] as const,
  /** Book counts change with every borrow and return, so these refresh too. */
  books: ['books'] as const,
};
