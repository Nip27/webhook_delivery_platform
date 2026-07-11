import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Badge from './Badge';

describe('Badge', () => {
  it('renders the requested semantic color', () => {
    render(<Badge color="green">SUCCESS</Badge>);

    expect(screen.getByText('SUCCESS')).toHaveClass('bg-green-100', 'text-green-800');
  });

  it('falls back to gray for an unknown color', () => {
    render(<Badge color="purple">UNKNOWN</Badge>);

    expect(screen.getByText('UNKNOWN')).toHaveClass('bg-gray-100', 'text-gray-800');
  });
});
