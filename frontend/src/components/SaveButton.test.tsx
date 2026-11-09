import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

const navigateMock = vi.fn();
vi.mock('react-router-dom', () => ({ useNavigate: () => navigateMock }));

const saveMock = vi.fn();
const removeMock = vi.fn();
vi.mock('../services', () => ({
  libraryService: { save: (...a: unknown[]) => saveMock(...a), remove: (...a: unknown[]) => removeMock(...a) },
  getApiErrorMessage: (_e: unknown, f: string) => f,
  hasStatus: () => false
}));

let authed = false;
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ isAuthenticated: authed }) }));

import { SaveButton } from './SaveButton';

describe('SaveButton', () => {
  beforeEach(() => {
    navigateMock.mockClear();
    saveMock.mockReset();
    removeMock.mockReset();
  });

  it('sends anonymous users to login instead of saving', async () => {
    authed = false;
    render(<SaveButton bookId={2} />);
    await userEvent.click(screen.getByRole('button'));
    expect(navigateMock).toHaveBeenCalledWith('/login');
    expect(saveMock).not.toHaveBeenCalled();
  });

  it('saves for authenticated users and reflects the saved state', async () => {
    authed = true;
    saveMock.mockResolvedValue({});
    render(<SaveButton bookId={2} />);
    await userEvent.click(screen.getByRole('button', { name: /save/i }));
    expect(saveMock).toHaveBeenCalledWith(2);
    expect(await screen.findByRole('button', { name: /saved/i })).toBeInTheDocument();
  });
});
