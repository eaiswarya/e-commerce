import { useMutation } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router';
import { ApiError } from '../../api/client';
import { errorMessage } from '../../api/errors';
import { useAuth } from '../../auth/authContext';
import { ErrorBanner } from '../../components/ErrorBanner';
import { FormField } from '../../components/FormField';
import styles from './LoginPage.module.css';

interface Credentials {
  username: string;
  password: string;
}

type FieldErrors = Partial<Record<keyof Credentials, string>>;

function validate({ username, password }: Credentials): FieldErrors {
  const errors: FieldErrors = {};
  if (!username.trim()) {
    errors.username = 'Enter your username';
  }
  if (!password) {
    errors.password = 'Enter your password';
  }
  return errors;
}

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const location = useLocation();
  const [credentials, setCredentials] = useState<Credentials>({ username: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const mutation = useMutation({
    mutationFn: ({ username, password }: Credentials) => login(username.trim(), password),
    onError: (error) => {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors);
      }
    },
  });

  // Once logged in (now or already), go back to the page that sent the user here.
  if (isAuthenticated) {
    const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
    return <Navigate to={from && from !== '/login' ? from : '/'} replace />;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors = validate(credentials);
    setFieldErrors(errors);
    if (Object.keys(errors).length === 0) {
      mutation.mutate(credentials);
    }
  }

  function update(field: keyof Credentials, value: string) {
    setCredentials((current) => ({ ...current, [field]: value }));
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit} noValidate>
        <h1 className={styles.title}>Library sign in</h1>
        {mutation.isError && <ErrorBanner message={errorMessage(mutation.error)} />}
        <FormField
          label="Username"
          name="username"
          autoComplete="username"
          value={credentials.username}
          error={fieldErrors.username}
          onChange={(event) => update('username', event.target.value)}
        />
        <FormField
          label="Password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={credentials.password}
          error={fieldErrors.password}
          onChange={(event) => update('password', event.target.value)}
        />
        <button type="submit" className={styles.submit} disabled={mutation.isPending}>
          {mutation.isPending ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  );
}
