import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { login } from '../../api/auth';
import { ApiError } from '../../api/client';
import { saveSession } from '../../auth/session';
import { renderWithProviders } from '../../test/render';
import { LoginPage } from './LoginPage';

vi.mock('../../api/auth', () => ({ login: vi.fn() }));

const future = () => new Date(Date.now() + 60 * 60 * 1000).toISOString();

function renderLogin(from?: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<p>Home screen</p>} />
      <Route path="/books" element={<p>Books screen</p>} />
    </Routes>,
    { route: from ? { pathname: '/login', state: { from: { pathname: from } } } : '/login' },
  );
}

async function signIn(username: string, password: string) {
  if (username) await userEvent.type(screen.getByLabelText('Username'), username);
  if (password) await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
}

beforeEach(() => {
  vi.mocked(login).mockReset();
});

describe('LoginPage', () => {
  it('asks for both fields without calling the API when they are empty', async () => {
    renderLogin();

    await signIn('', '');

    expect(screen.getByLabelText('Username')).toHaveAccessibleDescription('Enter your username');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Enter your password');
    expect(login).not.toHaveBeenCalled();
  });

  it('disables the button while signing in', async () => {
    vi.mocked(login).mockReturnValue(new Promise(() => {}));
    renderLogin();

    await signIn('admin', 'admin123');

    expect(screen.getByRole('button', { name: 'Signing in…' })).toBeDisabled();
  });

  it('signs in and opens the home screen', async () => {
    vi.mocked(login).mockResolvedValue({ token: 't', expiresAt: future(), fullName: 'Administrator' });
    renderLogin();

    await signIn(' admin ', 'admin123');

    expect(await screen.findByText('Home screen')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('admin', 'admin123');
  });

  it('returns to the page that required login', async () => {
    vi.mocked(login).mockResolvedValue({ token: 't', expiresAt: future(), fullName: 'Administrator' });
    renderLogin('/books');

    await signIn('admin', 'admin123');

    expect(await screen.findByText('Books screen')).toBeInTheDocument();
  });

  it('shows the server message for wrong credentials and stays on the form', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, 'UNAUTHORIZED', 'Invalid username or password'));
    renderLogin();

    await signIn('admin', 'wrong');

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password');
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
  });

  it('explains when the server cannot be reached', async () => {
    vi.mocked(login).mockRejectedValue(
      new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.'),
    );
    renderLogin();

    await signIn('admin', 'admin123');

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server.');
  });

  it('shows field errors returned by the API', async () => {
    vi.mocked(login).mockRejectedValue(
      new ApiError(400, 'VALIDATION_FAILED', 'Request validation failed', {
        username: 'size must be at most 50',
      }),
    );
    renderLogin();

    await signIn('admin', 'admin123');

    expect(await screen.findByText('size must be at most 50')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Please correct the highlighted fields.');
  });

  it('sends an already signed-in user straight to the home screen', () => {
    saveSession({ token: 't', expiresAt: future(), username: 'admin', fullName: 'Administrator' });

    renderLogin();

    expect(screen.getByText('Home screen')).toBeInTheDocument();
  });
});
