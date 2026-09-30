import { describe, expect, it } from 'vitest';
import { ApiError } from './client';
import { errorMessage } from './errors';

describe('errorMessage', () => {
  it.each([
    ['NO_COPIES_AVAILABLE', 'No copies of this book are available right now.'],
    ['LOAN_LIMIT_REACHED', 'This member already has the maximum number of books on loan.'],
    ['MEMBER_HAS_OVERDUE', 'This member has overdue books and must return them before borrowing more.'],
    [
      'CONCURRENT_UPDATE',
      'Someone else changed this record while you were editing. Reload it and try again.',
    ],
    [
      'HAS_LOAN_HISTORY',
      'This book has been borrowed before, so it is kept for loan history. Set its total copies to 0 to withdraw it.',
    ],
  ])('explains %s in plain language', (code, message) => {
    expect(errorMessage(new ApiError(409, code, 'server wording'))).toBe(message);
  });

  it('uses the server message for codes it does not know, such as a failed login', () => {
    expect(errorMessage(new ApiError(401, 'UNAUTHORIZED', 'Invalid username or password'))).toBe(
      'Invalid username or password',
    );
  });

  it('asks the user to fix the highlighted fields on a validation error', () => {
    expect(errorMessage(new ApiError(400, 'VALIDATION_FAILED', 'Request validation failed'))).toBe(
      'Please correct the highlighted fields.',
    );
  });

  it('keeps the network message', () => {
    expect(errorMessage(new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.'))).toBe(
      'Cannot reach the server.',
    );
  });

  it('gives a generic message for anything that is not an ApiError', () => {
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong. Please try again.');
    expect(errorMessage(undefined)).toBe('Something went wrong. Please try again.');
  });
});
