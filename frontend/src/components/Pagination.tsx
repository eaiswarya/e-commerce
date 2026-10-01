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
      <button
        type="button"
        className={styles.button}
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className={styles.button}
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}
