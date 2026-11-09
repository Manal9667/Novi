import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorState } from './ErrorState';

describe('ErrorState', () => {
  it('renders the message as an alert', () => {
    render(<ErrorState message="Something broke" />);
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Something broke');
  });

  it('invokes the retry handler when provided', async () => {
    const onRetry = vi.fn();
    render(<ErrorState message="Failed" onRetry={onRetry} />);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders no retry button when no handler is given', () => {
    render(<ErrorState message="Failed" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
