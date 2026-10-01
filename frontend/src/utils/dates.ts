/**
 * Formats a calendar date such as a due date ({@code 2026-10-13}). Building it from its parts keeps the same day in
 * every time zone; {@code new Date('2026-10-13')} would be UTC midnight and show the day before west of UTC.
 */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString();
}

/** Formats an instant (e.g. when a book was borrowed) as a local date. */
export function formatDateTime(instant: string): string {
  return new Date(instant).toLocaleDateString();
}
