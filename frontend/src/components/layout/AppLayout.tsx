import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../../auth/authContext';
import styles from './AppLayout.module.css';

/** Frame for every signed-in page: navigation, the librarian's name and a way to log out. */
export function AppLayout() {
  const { user, logout } = useAuth();
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <span className={styles.brand}>Library</span>
        <nav aria-label="Main" className={styles.nav}>
          {/* NavLink marks the current section with aria-current="page". */}
          <NavLink to="/" end className={styles.link}>
            Dashboard
          </NavLink>
          <NavLink to="/books" className={styles.link}>
            Books
          </NavLink>
          <NavLink to="/members" className={styles.link}>
            Members
          </NavLink>
          <NavLink to="/loans" className={styles.link}>
            Loans
          </NavLink>
        </nav>
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
