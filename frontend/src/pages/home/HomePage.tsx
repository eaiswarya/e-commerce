import { useAuth } from '../../auth/authContext';

/** Placeholder landing page; the dashboard replaces it in the frontend-pages PR. */
export function HomePage() {
  const { user } = useAuth();
  return (
    <section>
      <h1>Welcome, {user?.fullName}</h1>
      <p>Books, members and loans will appear here.</p>
    </section>
  );
}
