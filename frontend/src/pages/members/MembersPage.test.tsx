import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';
import { ApiError } from '../../api/client';
import { searchMembers } from '../../api/members';
import { renderWithProviders } from '../../test/render';
import type { Member, PageResponse } from '../../types';
import { MembersPage } from './MembersPage';

vi.mock('../../api/members', () => ({
  searchMembers: vi.fn(),
  createMember: vi.fn(),
  updateMember: vi.fn(),
  getMember: vi.fn(),
}));

const ADA: Member = {
  id: 7,
  memberCode: 'M0007',
  fullName: 'Ada Lovelace',
  email: 'ada@example.com',
  phone: null,
  active: false,
  joinedAt: '2026-09-29T10:15:30Z',
  version: 2,
};

function page(content: Member[], extra: Partial<PageResponse<Member>> = {}): PageResponse<Member> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    ...extra,
  };
}

function renderPage(route = '/members') {
  return renderWithProviders(
    <Routes>
      <Route path="/members" element={<MembersPage />} />
      <Route path="/members/:id" element={<p>Member detail</p>} />
    </Routes>,
    { route },
  );
}

const lastSearch = () => vi.mocked(searchMembers).mock.calls.at(-1)![0];

beforeEach(() => {
  vi.mocked(searchMembers).mockReset();
});

describe('MembersPage', () => {
  it('shows loading while members are fetched', () => {
    vi.mocked(searchMembers).mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Loading…');
  });

  it('lists members with their code, status and a link to each', async () => {
    vi.mocked(searchMembers).mockResolvedValue(page([ADA]));
    renderPage();

    const table = await screen.findByRole('table', { name: 'Members' });
    expect(within(table).getByText('M0007')).toBeInTheDocument();
    expect(within(table).getByText('Inactive')).toBeInTheDocument();
    await userEvent.click(within(table).getByRole('link', { name: 'Ada Lovelace' }));
    expect(screen.getByText('Member detail')).toBeInTheDocument();
  });

  it('shows the empty state', async () => {
    vi.mocked(searchMembers).mockResolvedValue(page([]));
    renderPage();

    expect(await screen.findByText('No members yet. Add the first one.')).toBeInTheDocument();
  });

  it('shows a load failure with Retry', async () => {
    vi.mocked(searchMembers)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'Unexpected error'))
      .mockResolvedValueOnce(page([ADA]));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on the server');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('table')).toBeInTheDocument();
  });

  it('maps the status filter to the API and keeps it in the URL', async () => {
    vi.mocked(searchMembers).mockResolvedValue(page([ADA]));
    renderPage('/members?status=inactive&q=ada');

    await screen.findByRole('table');
    expect(lastSearch()).toEqual({ q: 'ada', active: false, page: 0 });

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'active');
    await waitFor(() => expect(lastSearch()).toEqual({ q: 'ada', active: true, page: 0 }));

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'all');
    await waitFor(() => expect(lastSearch()).toEqual({ q: 'ada', active: undefined, page: 0 }));
  });

  it('searches by name, email or code as the librarian types', async () => {
    vi.mocked(searchMembers).mockResolvedValue(page([ADA]));
    renderPage();
    await screen.findByRole('table');

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search' }), 'M0007');

    await waitFor(() => expect(lastSearch()).toMatchObject({ q: 'M0007' }));
  });

  it('opens the add form', async () => {
    vi.mocked(searchMembers).mockResolvedValue(page([]));
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Add member' }));

    expect(screen.getByRole('dialog', { name: 'Add member' })).toBeInTheDocument();
  });
});
