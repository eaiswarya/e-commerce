import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from './client';
import { createMember, deactivateMember, getMember, searchMembers, updateMember } from './members';

vi.mock('./client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./client')>()),
  apiFetch: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(apiFetch).mockResolvedValue({});
});

describe('members api', () => {
  it('sends the active filter as true, false or not at all', async () => {
    await searchMembers({ q: 'ada', active: true });
    await searchMembers({ active: false, page: 1 });
    await searchMembers({});

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/members?q=ada&active=true', { signal: undefined });
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/members?active=false&page=1', { signal: undefined });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/api/members', { signal: undefined });
  });

  it('reads, creates, updates and deactivates', async () => {
    const input = { fullName: 'Ada Lovelace', email: 'ada@example.com', phone: null };

    await getMember(7);
    await createMember(input);
    await updateMember(7, { ...input, version: 2 });
    await deactivateMember(7);

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/members/7', { signal: undefined });
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/members', { method: 'POST', body: input });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/api/members/7', {
      method: 'PUT',
      body: { ...input, version: 2 },
    });
    expect(apiFetch).toHaveBeenNthCalledWith(4, '/api/members/7/deactivate', { method: 'PATCH' });
  });
});
