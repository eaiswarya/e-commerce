import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { login as loginRequest } from '../api/auth';
import { onUnauthorized, setAuthToken } from '../api/client';
import { AuthContext, type AuthContextValue } from './authContext';
import { clearSession, loadSession, saveSession, type Session } from './session';

function restoreSession(): Session | null {
  const session = loadSession();
  setAuthToken(session?.token ?? null);
  return session;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session | null>(restoreSession);

  const logout = useCallback(() => {
    clearSession();
    setAuthToken(null);
    setSession(null);
    // Nothing loaded for this librarian should be visible to the next one.
    queryClient.clear();
  }, [queryClient]);

  const login = useCallback(async (username: string, password: string) => {
    const response = await loginRequest(username, password);
    const next: Session = {
      token: response.token,
      expiresAt: response.expiresAt,
      username,
      fullName: response.fullName,
    };
    saveSession(next);
    setAuthToken(next.token);
    setSession(next);
  }, []);

  // An expired or rejected token on any API call ends the session; ProtectedRoute then sends the user to /login.
  useEffect(() => onUnauthorized(logout), [logout]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session ? { username: session.username, fullName: session.fullName } : null,
      isAuthenticated: session !== null,
      login,
      logout,
    }),
    [session, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
