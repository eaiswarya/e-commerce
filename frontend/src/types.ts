/** Error body returned by the API's GlobalExceptionHandler. */
export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
  timestamp: string;
}

/** Every list endpoint returns this page shape. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  fullName: string;
}

export interface CurrentUser {
  username: string;
  fullName: string;
}

export interface Book {
  id: number;
  isbn: string;
  title: string;
  author: string;
  category: string | null;
  publishedYear: number | null;
  totalCopies: number;
  availableCopies: number;
  /** Send back unchanged on the next update; a stale value is rejected with 409 CONCURRENT_UPDATE. */
  version: number;
}

/** Create/update payload; {@code version} is required on update. */
export interface BookInput {
  isbn: string;
  title: string;
  author: string;
  category: string | null;
  publishedYear: number | null;
  totalCopies: number;
  version?: number;
}

export interface Member {
  id: number;
  memberCode: string;
  fullName: string;
  email: string;
  phone: string | null;
  active: boolean;
  joinedAt: string;
  version: number;
}

export interface MemberInput {
  fullName: string;
  email: string;
  phone: string | null;
  version?: number;
}
