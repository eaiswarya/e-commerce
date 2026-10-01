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
      <span>{message}</span>
      {onRetry && (
        <button type="button" className={styles.retry} onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}
