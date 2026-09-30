import { Outlet } from 'react-router';
import { useAuth } from '../../auth/authContext';
import styles from './AppLayout.module.css';

/** Frame for every signed-in page: header with the librarian's name and a way to log out. */
export function AppLayout() {
  const { user, logout } = useAuth();
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <span className={styles.brand}>Library</span>
        <div className={styles.account}>
          <span>{user?.fullName}</span>
          <button type="button" className={styles.logout} onClick={logout}>
            Log out
          </button>
        </div>
      </header>
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
