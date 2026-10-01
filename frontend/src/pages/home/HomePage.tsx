import { Link } from 'react-router';
import { useAuth } from '../../auth/authContext';

/** Placeholder landing page; the dashboard replaces it in the frontend-pages PR. */
export function HomePage() {
  const { user } = useAuth();
  return (
    <section>
      <h1>Welcome, {user?.fullName}</h1>
      <p>
        Go to <Link to="/books">Books</Link> or <Link to="/members">Members</Link>. The dashboard with loans
        arrives next.
      </p>
    </section>
  );
}
