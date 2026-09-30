import { z } from 'zod';

const sessionSchema = z.object({
  token: z.string(),
  expiresAt: z.iso.datetime({ offset: true }),
  username: z.string(),
  fullName: z.string(),
});

/** The logged-in librarian, kept in sessionStorage so a reload stays logged in but closing the tab logs out. */
export type Session = z.infer<typeof sessionSchema>;

const KEY = 'library.session';

/** Returns the stored session, or null (clearing it) when it is missing, corrupt or expired. */
export function loadSession(now: number = Date.now()): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) {
      return null;
    }
    const parsed = sessionSchema.safeParse(JSON.parse(raw));
    if (parsed.success && Date.parse(parsed.data.expiresAt) > now) {
      return parsed.data;
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
