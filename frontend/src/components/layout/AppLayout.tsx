import {
  ArrowLeftRight,
  BookOpen,
  LayoutDashboard,
  Library,
  LogOut,
  Users,
  type LucideIcon,
} from 'lucide-react';
import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../../auth/authContext';
import styles from './AppLayout.module.css';

const NAV: { to: string; label: string; icon: LucideIcon; end?: boolean }[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/books', label: 'Books', icon: BookOpen },
  { to: '/members', label: 'Members', icon: Users },
  { to: '/loans', label: 'Loans', icon: ArrowLeftRight },
];

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

/** Frame for every signed-in page: sidebar navigation (a top bar on small screens), the librarian and log out. */
export function AppLayout() {
  const { user, logout } = useAuth();
  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.logo}>
            <Library />
          </span>
          Library
        </div>
        <nav aria-label="Main" className={styles.nav}>
          {/* NavLink marks the current section with aria-current="page". */}
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={styles.link}>
              <Icon className={styles.linkIcon} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className={styles.account}>
          <span className={styles.avatar} aria-hidden="true">
            {initials(user?.fullName ?? '?')}
          </span>
          <span className={styles.who}>
            <span className={styles.name}>{user?.fullName}</span>
            <span className={styles.username}>{user?.username}</span>
          </span>
          <button
            type="button"
            className={`btn btn-ghost btn-sm ${styles.logout}`}
            onClick={logout}
            title="Log out"
          >
            <LogOut />
            <span className={styles.logoutText}>Log out</span>
          </button>
        </div>
      </aside>
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
