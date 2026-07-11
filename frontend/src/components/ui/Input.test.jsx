import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Input from './Input';

describe('Input Component', () => {
  it('renders correctly', () => {
    render(<Input placeholder="Enter text" />);
    expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
  });

  it('renders label when provided', () => {
    render(<Input label="Email Address" id="email" />);
    expect(screen.getByText('Email Address')).toBeInTheDocument();
  });

  it('renders error message and applies error styles', () => {
    render(<Input error="Invalid email" placeholder="email" />);
    expect(screen.getByText('Invalid email')).toBeInTheDocument();
    
    const input = screen.getByPlaceholderText('email');
    expect(input).toHaveClass('border-red-300');
  });

  it('handles onChange events', () => {
    const handleChange = vi.fn();
    render(<Input placeholder="Type here" onChange={handleChange} />);
    
    const input = screen.getByPlaceholderText('Type here');
    fireEvent.change(input, { target: { value: 'test value' } });
    
    expect(handleChange).toHaveBeenCalledTimes(1);
  });

  it('merges custom className', () => {
    render(<Input placeholder="custom" className="my-input-class" />);
    const input = screen.getByPlaceholderText('custom');
    expect(input).toHaveClass('my-input-class');
  });
});
