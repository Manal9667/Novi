import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StarRating } from './StarRating';

describe('StarRating', () => {
  it('renders interactive stars as keyboard-focusable buttons', () => {
    render(<StarRating value={0} onChange={() => {}} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(5);
    expect(buttons[0]).toHaveAccessibleName('Rate 1 star');
    expect(buttons[4]).toHaveAccessibleName('Rate 5 stars');
  });

  it('calls onChange with the chosen star count on click', async () => {
    const onChange = vi.fn();
    render(<StarRating value={0} onChange={onChange} />);
    await userEvent.click(screen.getByRole('button', { name: 'Rate 4 stars' }));
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('is operable via the keyboard', async () => {
    const onChange = vi.fn();
    render(<StarRating value={0} onChange={onChange} />);
    await userEvent.tab(); // focus first star
    await userEvent.keyboard('{Enter}');
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it('renders a non-interactive image with a summary label when read-only', () => {
    render(<StarRating value={3} readOnly />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByRole('img')).toHaveAccessibleName('Rated 3 out of 5 stars');
  });
});
