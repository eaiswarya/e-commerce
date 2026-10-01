import type { Book, BookInput, PageResponse } from '../types';
import { apiFetch, toQuery } from './client';

export interface BookSearch {
  q?: string;
  category?: string;
  /** Only {@code true} filters; {@code false} means "all books", as in the API. */
  available?: boolean;
  page?: number;
  size?: number;
}

export function searchBooks({ q, category, available, page, size }: BookSearch, signal?: AbortSignal) {
  const query = toQuery({ q, category, available: available || undefined, page: page || undefined, size });
  return apiFetch<PageResponse<Book>>(`/api/books${query}`, { signal });
}

export function getBook(id: number, signal?: AbortSignal) {
  return apiFetch<Book>(`/api/books/${id}`, { signal });
}

export function createBook(input: BookInput) {
  return apiFetch<Book>('/api/books', { method: 'POST', body: input });
}

export function updateBook(id: number, input: BookInput) {
  return apiFetch<Book>(`/api/books/${id}`, { method: 'PUT', body: input });
}

export function deleteBook(id: number) {
  return apiFetch<void>(`/api/books/${id}`, { method: 'DELETE' });
}
