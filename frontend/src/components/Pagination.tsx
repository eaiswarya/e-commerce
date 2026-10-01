import { ChevronLeft, ChevronRight } from 'lucide-react';
import styles from './Pagination.module.css';

interface PaginationProps {
  /** Zero-based, as in the API. */
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <nav className={styles.pagination} aria-label="Pagination">
      <span className={styles.status}>
        Page {page + 1} of {totalPages}
      </span>
      <div className={styles.buttons}>
        <button type="button" className="btn btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
          <ChevronLeft />
          Previous
        </button>
        <button
          type="button"
          className="btn btn-sm"
          disabled={page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          Next
          <ChevronRight />
        </button>
      </div>
    </nav>
  );
}
