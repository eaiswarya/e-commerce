import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/client';
import { createMember, getMember, updateMember } from '../../api/members';
import { renderWithProviders } from '../../test/render';
import type { Member } from '../../types';
import { MemberFormDialog } from './MemberFormDialog';

vi.mock('../../api/members', () => ({ createMember: vi.fn(), updateMember: vi.fn(), getMember: vi.fn() }));

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

function renderDialog(member?: Member) {
  const onOpenChange = vi.fn();
  renderWithProviders(<MemberFormDialog open onOpenChange={onOpenChange} member={member} />);
  return { onOpenChange };
}

async function fill(label: string, value: string) {
  const input = screen.getByLabelText(label);
  await userEvent.clear(input);
  if (value) await userEvent.type(input, value);
}

const save = () => userEvent.click(screen.getByRole('button', { name: 'Save' }));

beforeEach(() => {
  vi.mocked(createMember).mockReset();
  vi.mocked(updateMember).mockReset();
  vi.mocked(getMember).mockReset();
});

describe('MemberFormDialog', () => {
  it('checks the fields before sending anything', async () => {
    renderDialog();

    await fill('Email', 'not-an-email');
    await fill('Phone', '---');
    await save();

    expect(screen.getByLabelText('Full name')).toHaveAccessibleDescription('Enter the full name');
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter a valid email address');
    expect(screen.getByLabelText('Phone')).toHaveAccessibleDescription('Enter a phone number');
    expect(createMember).not.toHaveBeenCalled();
  });

  it('adds a member with a trimmed email and no phone', async () => {
    vi.mocked(createMember).mockResolvedValue(ADA);
    const { onOpenChange } = renderDialog();

    expect(screen.getByRole('dialog', { name: 'Add member' })).toBeInTheDocument();
    await fill('Full name', 'Ada Lovelace');
    await fill('Email', '  Ada@Example.com ');
    await save();

    expect(createMember).toHaveBeenCalledWith({
      fullName: 'Ada Lovelace',
      email: 'Ada@Example.com',
      phone: null,
    });
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(await screen.findByText('Ada Lovelace added as M0007')).toBeInTheDocument();
  });

  it('marks the email when another member already uses it', async () => {
    vi.mocked(createMember).mockRejectedValue(new ApiError(409, 'DUPLICATE', 'A member with email exists'));
    renderDialog();

    await fill('Full name', 'Ada Two');
    await fill('Email', 'ada@example.com');
    await save();

    expect(await screen.findByLabelText('Email')).toHaveAccessibleDescription(
      'A member with this email already exists',
    );
  });

  it('edits with the loaded version and reloads after a conflict', async () => {
    vi.mocked(updateMember)
      .mockRejectedValueOnce(new ApiError(409, 'CONCURRENT_UPDATE', 'Changed by someone else'))
      .mockResolvedValueOnce({ ...ADA, version: 4 });
    vi.mocked(getMember).mockResolvedValue({ ...ADA, fullName: 'Ada King', version: 3 });
    renderDialog(ADA);

    await fill('Phone', '');
    await save();
    expect(updateMember).toHaveBeenCalledWith(7, {
      fullName: 'Ada Lovelace',
      email: 'ada@example.com',
      phone: null,
      version: 2,
    });

    await userEvent.click(await screen.findByRole('button', { name: 'Reload latest' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Loaded the latest version of this member');
    expect(screen.getByLabelText('Full name')).toHaveValue('Ada King');

    await save();
    expect(updateMember).toHaveBeenLastCalledWith(
      7,
      expect.objectContaining({ fullName: 'Ada King', version: 3 }),
    );
  });
});
