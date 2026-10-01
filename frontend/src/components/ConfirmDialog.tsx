import type { ReactNode } from 'react';
import { errorMessage } from '../api/errors';
import { ErrorBanner } from './ErrorBanner';
import { Modal } from './Modal';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  isPending?: boolean;
  /** The failed attempt's error, shown in plain language so the user knows why nothing happened. */
  error?: unknown;
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  children,
  confirmLabel,
  onConfirm,
  isPending = false,
  error,
}: ConfirmDialogProps) {
  return (
    <Modal open={open} onOpenChange={onOpenChange} title={title}>
      {error ? <ErrorBanner message={errorMessage(error)} /> : null}
      <div>{children}</div>
      <div className="actions">
        <button type="button" className="btn" onClick={() => onOpenChange(false)}>
          Cancel
        </button>
        <button type="button" className="btn btn-danger" disabled={isPending} onClick={onConfirm}>
          {isPending ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
