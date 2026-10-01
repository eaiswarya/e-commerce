import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { ApiError } from '../../api/client';
import { errorMessage } from '../../api/errors';
import { createMember, getMember, updateMember } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import { ErrorBanner } from '../../components/ErrorBanner';
import { FormField } from '../../components/FormField';
import { Modal } from '../../components/Modal';
import type { Member, MemberInput } from '../../types';

/** Optional leading +, then digits, spaces, hyphens or parentheses with at least one digit: the API's rule. */
const PHONE = /^\+?(?=[^0-9]*[0-9])[0-9 ()-]{3,29}$/;

const schema = z.object({
  fullName: z.string().trim().min(1, 'Enter the full name').max(150, 'At most 150 characters'),
  // The API rejects an address with spaces around it, so trim before sending.
  email: z.string().trim().max(254, 'At most 254 characters').pipe(z.email('Enter a valid email address')),
  phone: z
    .string()
    .trim()
    .refine((value) => value === '' || PHONE.test(value), { message: 'Enter a phone number' })
    .transform((value) => value || null),
});

type FormValues = z.input<typeof schema>;

function toFormValues(member?: Member): FormValues {
  return { fullName: member?.fullName ?? '', email: member?.email ?? '', phone: member?.phone ?? '' };
}

interface MemberFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The member to edit; omit to add a new one. */
  member?: Member;
}

export function MemberFormDialog({ open, onOpenChange, member }: MemberFormDialogProps) {
  return (
    <Modal open={open} onOpenChange={onOpenChange} title={member ? 'Edit member' : 'Add member'}>
      <MemberForm member={member} onDone={() => onOpenChange(false)} />
    </Modal>
  );
}

function MemberForm({ member, onDone }: { member?: Member; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [version, setVersion] = useState(member?.version);
  const [reloaded, setReloaded] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues, unknown, z.output<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: toFormValues(member),
  });

  const save = useMutation({
    mutationFn: (input: MemberInput) =>
      member ? updateMember(member.id, { ...input, version }) : createMember(input),
    onSuccess: (saved) => {
      queryClient.setQueryData(queryKeys.member(saved.id), saved);
      void queryClient.invalidateQueries({ queryKey: queryKeys.memberSearches });
      toast.success(member ? 'Changes saved' : `${saved.fullName} added as ${saved.memberCode}`);
      onDone();
    },
    onError: (error) => {
      if (!(error instanceof ApiError)) return;
      if (error.code === 'DUPLICATE') {
        setError('email', { message: 'A member with this email already exists' });
      }
      Object.entries(error.fieldErrors).forEach(([field, message]) =>
        setError(field as keyof FormValues, { message }),
      );
    },
  });

  const reload = useMutation({
    mutationFn: () =>
      queryClient.fetchQuery({
        queryKey: queryKeys.member(member!.id),
        queryFn: () => getMember(member!.id),
        staleTime: 0,
      }),
    onSuccess: (latest) => {
      reset(toFormValues(latest));
      setVersion(latest.version);
      setReloaded(true);
      save.reset();
    },
  });

  const conflict = save.error instanceof ApiError && save.error.code === 'CONCURRENT_UPDATE';

  return (
    <form
      noValidate
      onSubmit={handleSubmit((values) => {
        setReloaded(false);
        save.mutate(values);
      })}
    >
      {save.isError && (
        <ErrorBanner
          message={errorMessage(save.error)}
          onRetry={conflict ? () => reload.mutate() : undefined}
          retryLabel="Reload latest"
        />
      )}
      {reload.isError && <ErrorBanner message={errorMessage(reload.error)} />}
      {reloaded && (
        <p role="status">
          Loaded the latest version of this member. Your unsaved changes were replaced; edit and save again.
        </p>
      )}
      <div className="form-grid">
        <FormField
          label="Full name"
          wide
          autoComplete="off"
          error={errors.fullName?.message}
          {...register('fullName')}
        />
        <FormField
          label="Email"
          type="email"
          autoComplete="off"
          error={errors.email?.message}
          {...register('email')}
        />
        <FormField
          label="Phone"
          type="tel"
          autoComplete="off"
          error={errors.phone?.message}
          {...register('phone')}
        />
      </div>
      <div className="actions">
        <button type="button" className="btn" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={save.isPending || reload.isPending}>
          {save.isPending ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  );
}
