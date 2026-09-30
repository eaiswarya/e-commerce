import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login } from '../api/auth';
import { apiFetch } from '../api/client';
import { renderWithProviders } from '../test/render';
import { useAuth } from './authContext';
import { loadSession, saveSession } from './session';

vi.mock('../api/auth', () => ({ login: vi.fn() }));

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
});

function Probe() {
  const { user, isAuthenticated, login: logIn, logout } = useAuth();
  return (
    <div>
      <p>{isAuthenticated ? `Signed in as ${user?.fullName} (${user?.username})` : 'Signed out'}</p>
      <button onClick={() => void logIn('admin', 'admin123')}>Log in</button>
      <button onClick={logout}>Log out</button>
    </div>
  );
}

const future = new Date(Date.now() + 60 * 60 * 1000).toISOString();

describe('AuthProvider', () => {
  it('starts signed out when nothing is stored', () => {
    renderWithProviders(<Probe />);

    expect(screen.getByText('Signed out')).toBeInTheDocument();
  });

  it('logs in, stores the session and sends the token on later API calls', async () => {
    vi.mocked(login).mockResolvedValue({ token: 'new.token', expiresAt: future, fullName: 'Administrator' });
    fetchMock.mockResolvedValue(new Response('{}', { status: 200 }));
    renderWithProviders(<Probe />);

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Signed in as Administrator (admin)')).toBeInTheDocument();
    expect(loadSession()).toMatchObject({ token: 'new.token', username: 'admin' });
    await apiFetch('/api/books');
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('Authorization')).toBe('Bearer new.token');
  });

  it('restores a stored session after a reload', () => {
    saveSession({ token: 'kept.token', expiresAt: future, username: 'admin', fullName: 'Administrator' });

    renderWithProviders(<Probe />);

    expect(screen.getByText('Signed in as Administrator (admin)')).toBeInTheDocument();
  });

  it('logs out and forgets the session', async () => {
    saveSession({ token: 'kept.token', expiresAt: future, username: 'admin', fullName: 'Administrator' });
    renderWithProviders(<Probe />);

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(screen.getByText('Signed out')).toBeInTheDocument();
    expect(loadSession()).toBeNull();
  });

  it('logs out when an API call with the token is rejected with 401', async () => {
    saveSession({ token: 'expired.token', expiresAt: future, username: 'admin', fullName: 'Administrator' });
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ status: 401, error: 'UNAUTHORIZED', message: 'Authentication required' }),
        {
          status: 401,
        },
      ),
    );
    renderWithProviders(<Probe />);

    await act(() => apiFetch('/api/books').catch(() => undefined));

    await waitFor(() => expect(screen.getByText('Signed out')).toBeInTheDocument());
    expect(loadSession()).toBeNull();
  });
});
