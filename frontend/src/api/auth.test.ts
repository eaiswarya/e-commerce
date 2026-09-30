import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchCurrentUser, login } from './auth';
import { apiFetch } from './client';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

beforeEach(() => {
  vi.mocked(apiFetch).mockResolvedValue({});
});

describe('auth api', () => {
  it('posts the credentials to the login endpoint', async () => {
    await login('admin', 'secret');

    expect(apiFetch).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      body: { username: 'admin', password: 'secret' },
    });
  });

  it('reads the current librarian', async () => {
    await fetchCurrentUser();

    expect(apiFetch).toHaveBeenCalledWith('/api/auth/me', { signal: undefined });
  });
});
