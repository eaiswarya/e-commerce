import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { searchBooks } from './api/books';
import { searchLoans } from './api/loans';
import { searchMembers } from './api/members';
import { saveSession } from './auth/session';
import { renderWithProviders } from './test/render';

vi.mock('./api/books', () => ({ searchBooks: vi.fn() }));
vi.mock('./api/members', () => ({ searchMembers: vi.fn() }));
vi.mock('./api/loans', () => ({ searchLoans: vi.fn() }));

const emptyPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

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

  it('opens the dashboard for a signed-in librarian', async () => {
    signInAsAdmin();
    [searchBooks, searchMembers, searchLoans].forEach((search) =>
      vi.mocked(search).mockResolvedValue(emptyPage),
    );

    renderWithProviders(<App />, { route: '/' });

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByText('Signed in as Administrator')).toBeInTheDocument();
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

  it.each([
    ['/books', 'Books', searchBooks],
    ['/members', 'Members', searchMembers],
    ['/loans', 'Loans', searchLoans],
  ])('routes %s to its page', async (route, heading, search) => {
    vi.mocked(search).mockResolvedValue(emptyPage);
    signInAsAdmin();

    renderWithProviders(<App />, { route });

    expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument();
  });
});
