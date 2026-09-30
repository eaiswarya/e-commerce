import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ErrorBanner } from './ErrorBanner';

describe('ErrorBanner', () => {
  it('announces the message', () => {
    render(<ErrorBanner message="Cannot reach the server." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Cannot reach the server.');
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
  });

  it('offers a retry when given one', async () => {
    const onRetry = vi.fn();
    render(<ErrorBanner message="Failed" onRetry={onRetry} />);

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(onRetry).toHaveBeenCalledOnce();
  });
});
