import type { CurrentUser, LoginResponse } from '../types';
import { apiFetch } from './client';

export function login(username: string, password: string): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/api/auth/login', { method: 'POST', body: { username, password } });
}

export function fetchCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/auth/me', { signal });
}
