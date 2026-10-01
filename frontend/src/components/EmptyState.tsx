import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

export function EmptyState({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className={styles.empty}>
      <p>{message}</p>
      {action}
    </div>
  );
}
