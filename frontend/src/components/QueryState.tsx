import type { UseQueryResult } from '@tanstack/react-query';
import { LoaderCircle } from 'lucide-react';
import type { ReactNode } from 'react';
import { ApiError } from '../api/client';
import { errorMessage } from '../api/errors';
import { EmptyState } from './EmptyState';
import { ErrorBanner } from './ErrorBanner';

interface QueryStateProps<T> {
  query: UseQueryResult<T>;
  /** Shown instead of the content when {@link isEmpty} says there is nothing to show. */
  empty?: ReactNode;
  isEmpty?: (data: T) => boolean;
  /** Shown instead of the error banner when the API answers 404, e.g. a detail page for a deleted record. */
  notFound?: ReactNode;
  children: (data: T) => ReactNode;
}

/** The loading, error (with Retry), empty and loaded states every data view needs, in one place. */
export function QueryState<T>({ query, empty, isEmpty, notFound, children }: QueryStateProps<T>) {
  if (query.isPending) {
    return (
      <p role="status" className="loading">
        <LoaderCircle className="spin" />
        Loading…
      </p>
    );
  }
  if (query.isError && notFound && query.error instanceof ApiError && query.error.status === 404) {
    return <>{notFound}</>;
  }
  if (query.isError) {
    return <ErrorBanner message={errorMessage(query.error)} onRetry={() => void query.refetch()} />;
  }
  if (isEmpty?.(query.data)) {
    return <>{empty ?? <EmptyState message="Nothing to show yet." />}</>;
  }
  return <>{children(query.data)}</>;
}
