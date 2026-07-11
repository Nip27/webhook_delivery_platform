import { describe, expect, it } from 'vitest';
import { statusBadgeColor, truncate } from './helpers';

describe('helpers', () => {
  it('maps delivery statuses to Badge colors', () => {
    expect(statusBadgeColor('SUCCESS')).toBe('green');
    expect(statusBadgeColor('FAILED')).toBe('red');
    expect(statusBadgeColor('PENDING')).toBe('yellow');
    expect(statusBadgeColor('unknown')).toBe('gray');
  });

  it('truncates long values', () => {
    expect(truncate('webhook-delivery', 7)).toBe('webhook...');
    expect(truncate('short', 10)).toBe('short');
  });
});
