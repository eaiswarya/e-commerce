import { CircleAlert } from 'lucide-react';
import styles from './ErrorBanner.module.css';

interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
  /** Label for the {@code onRetry} button; defaults to "Retry". */
  retryLabel?: string;
}

export function ErrorBanner({ message, onRetry, retryLabel = 'Retry' }: ErrorBannerProps) {
  return (
    <div role="alert" className={styles.banner}>
      <CircleAlert className={styles.icon} />
      <span className={styles.message}>{message}</span>
      {onRetry && (
        <button type="button" className="btn btn-sm" onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}
