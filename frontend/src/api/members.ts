import type { Member, MemberInput, PageResponse } from '../types';
import { apiFetch, toQuery } from './client';

export interface MemberSearch {
  q?: string;
  /** {@code true}: active only, {@code false}: inactive only, omitted: everyone. */
  active?: boolean;
  page?: number;
}

export function searchMembers({ q, active, page }: MemberSearch, signal?: AbortSignal) {
  return apiFetch<PageResponse<Member>>(`/api/members${toQuery({ q, active, page: page || undefined })}`, {
    signal,
  });
}

export function getMember(id: number, signal?: AbortSignal) {
  return apiFetch<Member>(`/api/members/${id}`, { signal });
}

export function createMember(input: MemberInput) {
  return apiFetch<Member>('/api/members', { method: 'POST', body: input });
}

export function updateMember(id: number, input: MemberInput) {
  return apiFetch<Member>(`/api/members/${id}`, { method: 'PUT', body: input });
}

export function deactivateMember(id: number) {
  return apiFetch<Member>(`/api/members/${id}/deactivate`, { method: 'PATCH' });
}
