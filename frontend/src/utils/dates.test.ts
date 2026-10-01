import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime } from './dates';

describe('dates', () => {
  it('formats a due date as that same calendar day', () => {
    expect(formatDate('2026-10-13')).toBe(new Date(2026, 9, 13).toLocaleDateString());
  });

  it('formats an instant in local time', () => {
    expect(formatDateTime('2026-09-29T10:15:30Z')).toBe(
      new Date('2026-09-29T10:15:30Z').toLocaleDateString(),
    );
  });
});
