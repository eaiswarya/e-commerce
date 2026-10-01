import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router';
import { saveSession } from '../../auth/session';
import { renderWithProviders } from '../../test/render';
import { AppLayout } from './AppLayout';

describe('AppLayout', () => {
  it('links to each section and marks the current one', () => {
    saveSession({
      token: 't',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      username: 'admin',
      fullName: 'Administrator',
    });

    renderWithProviders(
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/books" element={<p>Books</p>} />
        </Route>
      </Routes>,
      { route: '/books' },
    );

    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(nav).toHaveTextContent('DashboardBooksMembersLoans');
    expect(screen.getByRole('link', { name: 'Books' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
    expect(screen.getByText('Administrator')).toBeInTheDocument();
  });
});
