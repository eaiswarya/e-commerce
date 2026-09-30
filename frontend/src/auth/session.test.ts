import { describe, expect, it } from 'vitest';
import { clearSession, loadSession, saveSession, type Session } from './session';

const NOW = Date.parse('2026-09-30T10:00:00Z');
const SESSION: Session = {
  token: 'abc.def.ghi',
  expiresAt: '2026-09-30T18:00:00Z',
  username: 'admin',
  fullName: 'Administrator',
};

describe('session storage', () => {
  it('returns nothing when no one has logged in', () => {
    expect(loadSession(NOW)).toBeNull();
  });

  it('loads a saved session that has not expired', () => {
    saveSession(SESSION);

    expect(loadSession(NOW)).toEqual(SESSION);
  });

  it('drops an expired session', () => {
    saveSession(SESSION);

    expect(loadSession(Date.parse('2026-09-30T18:00:01Z'))).toBeNull();
    expect(sessionStorage.getItem('library.session')).toBeNull();
  });

  it('drops a corrupt or incomplete entry', () => {
    sessionStorage.setItem('library.session', '{not json');
    expect(loadSession(NOW)).toBeNull();

    sessionStorage.setItem('library.session', JSON.stringify({ token: 'x' }));
    expect(loadSession(NOW)).toBeNull();
    expect(sessionStorage.getItem('library.session')).toBeNull();
  });

  it('clears the session on logout', () => {
    saveSession(SESSION);

    clearSession();

    expect(loadSession(NOW)).toBeNull();
  });
});
