import { describe, expect, it } from 'vitest';
import { toQuery } from './client';

describe('toQuery', () => {
  it('returns an empty string when there is nothing to send', () => {
    expect(toQuery({})).toBe('');
    expect(toQuery({ q: '', category: '   ', page: undefined, x: null })).toBe('');
  });

  it('encodes and trims values', () => {
    expect(toQuery({ q: '  war & peace ', page: 2 })).toBe('?q=war+%26+peace&page=2');
  });

  it('keeps false, which is a real filter for some endpoints', () => {
    expect(toQuery({ active: false })).toBe('?active=false');
  });
});
