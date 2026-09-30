import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { saveSession } from './auth/session';
import { renderWithProviders } from './test/render';

function signInAsAdmin() {
  saveSession({
    token: 't',
    expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    username: 'admin',
    fullName: 'Administrator',
  });
}

describe('App', () => {
  it('sends a signed-out visitor to the login page', () => {
    renderWithProviders(<App />, { route: '/' });

    expect(screen.getByRole('heading', { name: 'Library sign in' })).toBeInTheDocument();
  });

  it('shows the signed-in librarian on the home page', () => {
    signInAsAdmin();

    renderWithProviders(<App />, { route: '/' });

    expect(screen.getByRole('heading', { name: 'Welcome, Administrator' })).toBeInTheDocument();
  });

  it('logs out back to the login page', async () => {
    signInAsAdmin();
    renderWithProviders(<App />, { route: '/' });

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(screen.getByRole('heading', { name: 'Library sign in' })).toBeInTheDocument();
  });

  it('shows not found for unknown pages inside the app', () => {
    signInAsAdmin();

    renderWithProviders(<App />, { route: '/nowhere' });

    expect(screen.getByText('Page not found.')).toBeInTheDocument();
  });
});
