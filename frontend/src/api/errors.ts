import { ApiError } from './client';

/** Plain-language text for the API's business-rule codes (see the design's error table). */
const MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: 'Please correct the highlighted fields.',
  NOT_FOUND: 'That record no longer exists.',
  NO_COPIES_AVAILABLE: 'No copies of this book are available right now.',
  LOAN_LIMIT_REACHED: 'This member already has the maximum number of books on loan.',
  MEMBER_HAS_OVERDUE: 'This member has overdue books and must return them before borrowing more.',
  MEMBER_INACTIVE: 'This member is inactive and cannot borrow books.',
  ALREADY_RETURNED: 'This loan has already been returned.',
  HAS_ACTIVE_LOANS: 'This book has copies on loan, so it cannot be deleted until they are returned.',
  HAS_LOAN_HISTORY:
    'This book has been borrowed before, so it is kept for loan history. Set its total copies to 0 to withdraw it.',
  COPIES_ON_LOAN: 'Total copies cannot be lower than the number of copies currently on loan.',
  DUPLICATE: 'A record with the same ISBN or email already exists.',
  CONCURRENT_UPDATE: 'Someone else changed this record while you were editing. Reload it and try again.',
  INTERNAL_ERROR: 'Something went wrong on the server. Please try again.',
};

/** A message to show the user for any error thrown by the API layer. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ?? error.message;
  }
  return 'Something went wrong. Please try again.';
}
