import type { Book, Loan, Member, PageResponse } from '../types';

export const BOOK: Book = {
  id: 3,
  isbn: '9780441013593',
  title: 'Dune',
  author: 'Frank Herbert',
  category: 'Fiction',
  publishedYear: 1965,
  totalCopies: 2,
  availableCopies: 1,
  version: 0,
};

export const MEMBER: Member = {
  id: 7,
  memberCode: 'M0007',
  fullName: 'Ada Lovelace',
  email: 'ada@example.com',
  phone: null,
  active: true,
  joinedAt: '2026-09-01T09:00:00Z',
  version: 0,
};

export const LOAN: Loan = {
  id: 11,
  bookId: BOOK.id,
  bookTitle: BOOK.title,
  bookIsbn: BOOK.isbn,
  memberId: MEMBER.id,
  memberCode: MEMBER.memberCode,
  memberName: MEMBER.fullName,
  borrowedAt: '2026-09-15T09:00:00Z',
  dueDate: '2026-09-29',
  returnedAt: null,
  status: 'OVERDUE',
};

export function pageOf<T>(content: T[], extra: Partial<PageResponse<T>> = {}): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    ...extra,
  };
}
