import type { BookSearch } from './books';
import type { MemberSearch } from './members';

/** Every query key in one place, so a mutation can invalidate exactly what it changed. */
export const queryKeys = {
  books: ['books'] as const,
  bookSearch: (search: BookSearch) => ['books', 'search', search] as const,
  book: (id: number) => ['books', 'detail', id] as const,
  members: ['members'] as const,
  memberSearch: (search: MemberSearch) => ['members', 'search', search] as const,
  member: (id: number) => ['members', 'detail', id] as const,
};
