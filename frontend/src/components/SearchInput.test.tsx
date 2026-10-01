import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SearchInput } from './SearchInput';

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('SearchInput', () => {
  it('searches once typing pauses, with the trimmed text', () => {
    const onSearch = vi.fn();
    render(<SearchInput label="Search books" value="" onSearch={onSearch} />);
    const input = screen.getByRole('searchbox', { name: 'Search books' });

    fireEvent.change(input, { target: { value: 'du' } });
    act(() => vi.advanceTimersByTime(200));
    fireEvent.change(input, { target: { value: ' dune ' } });
    act(() => vi.advanceTimersByTime(299));
    expect(onSearch).not.toHaveBeenCalled();

    act(() => vi.advanceTimersByTime(1));
    expect(onSearch).toHaveBeenCalledOnce();
    expect(onSearch).toHaveBeenCalledWith('dune');
  });

  it('shows a new value given from outside, e.g. after Back', () => {
    const { rerender } = render(<SearchInput label="Search" value="dune" onSearch={vi.fn()} />);

    rerender(<SearchInput label="Search" value="emma" onSearch={vi.fn()} />);

    expect(screen.getByRole('searchbox')).toHaveValue('emma');
  });
});
