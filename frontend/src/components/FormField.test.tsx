import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FormField } from './FormField';

describe('FormField', () => {
  it('labels the input', () => {
    render(<FormField label="Username" name="username" />);

    expect(screen.getByLabelText('Username')).toHaveAttribute('name', 'username');
  });

  it('marks the input invalid and links the error message to it', () => {
    render(<FormField label="Username" error="Enter your username" />);

    const input = screen.getByLabelText('Username');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter your username');
  });
});
