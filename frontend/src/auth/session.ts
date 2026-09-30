/** The logged-in librarian, kept in sessionStorage so a reload stays logged in but closing the tab logs out. */
export interface Session {
  token: string;
  expiresAt: string;
  username: string;
  fullName: string;
}

const KEY = 'library.session';

/** Returns the stored session, or null (clearing it) when it is missing, corrupt or expired. */
export function loadSession(now: number = Date.now()): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Partial<Session>;
    if (
      typeof session.token === 'string' &&
      typeof session.username === 'string' &&
      typeof session.fullName === 'string' &&
      typeof session.expiresAt === 'string' &&
      Date.parse(session.expiresAt) > now
    ) {
      return session as Session;
    }
  } catch {
    // Corrupt JSON or storage unavailable: treat as logged out.
  }
  clearSession();
  return null;
}

export function saveSession(session: Session): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    // Storage unavailable (e.g. privacy mode): the session still lives in memory until reload.
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // Nothing stored, nothing to clear.
  }
}
