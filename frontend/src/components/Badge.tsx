import styles from './Badge.module.css';

export function Badge({ tone, children }: { tone: 'success' | 'muted' | 'danger'; children: string }) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>;
}
