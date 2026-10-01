import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { ConfirmDialog } from './ConfirmDialog';

function renderDialog(props: { isPending?: boolean; error?: unknown } = {}) {
  const handlers = { onOpenChange: vi.fn(), onConfirm: vi.fn() };
  render(
    <ConfirmDialog open title="Delete book?" confirmLabel="Delete" {...handlers} {...props}>
      Dune will be removed.
    </ConfirmDialog>,
  );
  return handlers;
}

describe('ConfirmDialog', () => {
  it('asks for confirmation in an accessible dialog', async () => {
    const { onConfirm } = renderDialog();

    expect(screen.getByRole('dialog', { name: 'Delete book?' })).toHaveTextContent('Dune will be removed.');
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('closes on Cancel without confirming', async () => {
    const { onConfirm, onOpenChange } = renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('disables the action while it runs', () => {
    renderDialog({ isPending: true });

    expect(screen.getByRole('button', { name: 'Working…' })).toBeDisabled();
  });

  it('explains why the action failed', () => {
    renderDialog({ error: new ApiError(409, 'HAS_ACTIVE_LOANS', 'Book 7 has 1 copies on loan') });

    expect(screen.getByRole('alert')).toHaveTextContent('This book has copies on loan');
  });
});
