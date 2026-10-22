import { describe, expect, it } from 'vitest';
import { bytes, pct } from './format.js';

describe('format', () => {
  it('shows Unavailable instead of inventing values', () => {
    expect(pct(null)).toBe('Unavailable');
    expect(bytes(undefined)).toBe('Unavailable');
  });

  it('formats numbers for people', () => {
    expect(pct(42.4)).toBe('42%');
    expect(bytes(1536)).toBe('2 KB');
    expect(bytes(3 * 1024 ** 3)).toBe('3.0 GB');
  });
});
