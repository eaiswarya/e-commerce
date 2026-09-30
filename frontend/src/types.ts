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
