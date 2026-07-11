import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { useAsync } from './useAsync';

describe('useAsync', () => {
  it('should initialize with default state', () => {
    const mockFn = vi.fn();
    const { result } = renderHook(() => useAsync(mockFn));

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe(null);
    expect(result.current.data).toBe(null);
  });

  it('should handle successful execution', async () => {
    const mockFn = vi.fn().mockResolvedValue('success data');
    const { result } = renderHook(() => useAsync(mockFn));

    let promise;
    act(() => {
      promise = result.current.execute();
    });

    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBe(null);

    await act(async () => {
      await promise;
    });

    expect(result.current.loading).toBe(false);
    expect(result.current.data).toBe('success data');
    expect(result.current.error).toBe(null);
    expect(mockFn).toHaveBeenCalledTimes(1);
  });

  it('should handle failed execution', async () => {
    const mockFn = vi.fn().mockRejectedValue(new Error('test error'));
    const { result } = renderHook(() => useAsync(mockFn));

    let promise;
    act(() => {
      promise = result.current.execute();
    });

    expect(result.current.loading).toBe(true);

    await act(async () => {
      try {
        await promise;
      } catch (e) {
        // expected
      }
    });

    expect(result.current.loading).toBe(false);
    expect(result.current.data).toBe(null);
    expect(result.current.error).toBe('test error');
  });

  it('should extract error from axios response if available', async () => {
    const axiosError = {
      response: {
        data: { message: 'axios error message' }
      }
    };
    const mockFn = vi.fn().mockRejectedValue(axiosError);
    const { result } = renderHook(() => useAsync(mockFn));

    await act(async () => {
      try {
        await result.current.execute();
      } catch (e) {
        // expected
      }
    });

    expect(result.current.error).toBe('axios error message');
  });
});
