import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation } from 'react-router';
import { z } from 'zod';
import { ApiError } from '../../api/client';
import { errorMessage } from '../../api/errors';
import { useAuth } from '../../auth/authContext';
import { ErrorBanner } from '../../components/ErrorBanner';
import { FormField } from '../../components/FormField';
import styles from './LoginPage.module.css';

const schema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
});

type Credentials = z.infer<typeof schema>;

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Credentials>({ resolver: zodResolver(schema), defaultValues: { username: '', password: '' } });
  const mutation = useMutation({
    mutationFn: ({ username, password }: Credentials) => login(username, password),
    onError: (error) => {
      if (error instanceof ApiError) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          setError(field as keyof Credentials, { message }),
        );
      }
    },
  });

  // Once logged in (now or already), go back to the page that sent the user here.
  if (isAuthenticated) {
    const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
    return <Navigate to={from && from !== '/login' ? from : '/'} replace />;
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <h1 className={styles.title}>Library sign in</h1>
        {mutation.isError && <ErrorBanner message={errorMessage(mutation.error)} />}
        <FormField
          label="Username"
          autoComplete="username"
          error={errors.username?.message}
          {...register('username')}
        />
        <FormField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <button type="submit" className={styles.submit} disabled={mutation.isPending}>
          {mutation.isPending ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  );
}
