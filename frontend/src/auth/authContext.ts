import { createContext, useContext } from 'react';
import type { CurrentUser } from '../types';

export interface AuthContextValue {
  user: CurrentUser | null;
  isAuthenticated: boolean;
  /** Logs in and stores the session; rejects with the API's error on failure. */
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return value;
}
