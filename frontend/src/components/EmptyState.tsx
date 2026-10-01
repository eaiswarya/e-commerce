import { Inbox, type LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

interface EmptyStateProps {
  message: string;
  action?: ReactNode;
  icon?: LucideIcon;
}

export function EmptyState({ message, action, icon: Icon = Inbox }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <span className={styles.icon}>
        <Icon />
      </span>
      <p className={styles.message}>{message}</p>
      {action}
    </div>
  );
}
