import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { ApiError } from '../../api/client';
import { deactivateMember, getMember } from '../../api/members';
import { renderWithProviders } from '../../test/render';
import type { Member } from '../../types';
import { MemberDetailPage } from './MemberDetailPage';

vi.mock('../../api/members', () => ({
  getMember: vi.fn(),
  deactivateMember: vi.fn(),
  updateMember: vi.fn(),
  createMember: vi.fn(),
}));

const ADA: Member = {
  id: 7,
  memberCode: 'M0007',
  fullName: 'Ada Lovelace',
  email: 'ada@example.com',
  phone: '555-0100',
  active: true,
  joinedAt: '2026-09-29T10:15:30Z',
  version: 2,
};

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/members/:id" element={<MemberDetailPage />} />
    </Routes>,
    { route: '/members/7' },
  );
}

beforeEach(() => {
  vi.mocked(getMember).mockReset();
  vi.mocked(deactivateMember).mockReset();
});

describe('MemberDetailPage', () => {
  it('shows the member', async () => {
    vi.mocked(getMember).mockResolvedValue(ADA);
    renderPage();

    expect(await screen.findByRole('heading', { name: /Ada Lovelace/ })).toHaveTextContent('Active');
    expect(screen.getByText('M0007')).toBeInTheDocument();
    expect(screen.getByText(new Date(ADA.joinedAt).toLocaleDateString())).toBeInTheDocument();
  });

  it('says so when the member does not exist', async () => {
    vi.mocked(getMember).mockRejectedValue(new ApiError(404, 'NOT_FOUND', 'Member 7 not found'));
    renderPage();

    expect(await screen.findByText('Member not found.')).toBeInTheDocument();
  });

  it('deactivates after confirmation and then hides the button', async () => {
    vi.mocked(getMember).mockResolvedValue(ADA);
    vi.mocked(deactivateMember).mockResolvedValue({ ...ADA, active: false, version: 3 });
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Deactivate' }));
    expect(screen.getByRole('dialog', { name: 'Deactivate member?' })).toHaveTextContent(
      'will no longer be able to borrow books',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Deactivate' }));

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Ada Lovelace/ })).toHaveTextContent('Inactive'),
    );
    expect(screen.queryByRole('button', { name: 'Deactivate' })).not.toBeInTheDocument();
    expect(deactivateMember).toHaveBeenCalledWith(7);
  });

  it('does not offer deactivation for an inactive member', async () => {
    vi.mocked(getMember).mockResolvedValue({ ...ADA, active: false });
    renderPage();

    expect(await screen.findByRole('heading', { name: /Ada Lovelace/ })).toHaveTextContent('Inactive');
    expect(screen.queryByRole('button', { name: 'Deactivate' })).not.toBeInTheDocument();
  });

  it('opens the edit form with the member', async () => {
    vi.mocked(getMember).mockResolvedValue(ADA);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }));

    expect(screen.getByRole('dialog', { name: 'Edit member' })).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveValue('ada@example.com');
  });
});
