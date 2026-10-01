import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('is hidden when everything fits on one page', () => {
    const { container } = render(<Pagination page={0} totalPages={1} onChange={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('disables Previous on the first page and moves forward', async () => {
    const onChange = vi.fn();
    render(<Pagination page={0} totalPages={3} onChange={onChange} />);

    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(onChange).toHaveBeenCalledWith(1);
  });

  it('disables Next on the last page and moves back', async () => {
    const onChange = vi.fn();
    render(<Pagination page={2} totalPages={3} onChange={onChange} />);

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));

    expect(onChange).toHaveBeenCalledWith(1);
  });
});
