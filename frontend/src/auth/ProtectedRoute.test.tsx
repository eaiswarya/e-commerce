import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Route, Routes, useLocation } from 'react-router';
import { renderWithProviders } from '../test/render';
import { ProtectedRoute } from './ProtectedRoute';
import { saveSession } from './session';

function LoginStub() {
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
  return <p>Login page, return to {from}</p>;
}

function routes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginStub />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/books" element={<p>Books list</p>} />
      </Route>
    </Routes>
  );
}

describe('ProtectedRoute', () => {
  it('sends a signed-out user to /login and remembers the page they wanted', () => {
    renderWithProviders(routes(), { route: '/books' });

    expect(screen.getByText('Login page, return to /books')).toBeInTheDocument();
    expect(screen.queryByText('Books list')).not.toBeInTheDocument();
  });

  it('shows the page to a signed-in user', () => {
    saveSession({
      token: 't',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      username: 'admin',
      fullName: 'Administrator',
    });

    renderWithProviders(routes(), { route: '/books' });

    expect(screen.getByText('Books list')).toBeInTheDocument();
  });
});
