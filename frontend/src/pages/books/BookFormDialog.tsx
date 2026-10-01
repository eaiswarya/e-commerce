import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { createBook, getBook, updateBook } from '../../api/books';
import { ApiError } from '../../api/client';
import { errorMessage } from '../../api/errors';
import { queryKeys } from '../../api/queryKeys';
import { ErrorBanner } from '../../components/ErrorBanner';
import { FormField } from '../../components/FormField';
import { Modal } from '../../components/Modal';
import type { Book, BookInput } from '../../types';

/** ISBN-10 (last character may be X) or ISBN-13, single hyphens or spaces allowed: the API's own rule. */
const ISBN = /^(?:\d[- ]?){9}[\dXx]$|^(?:\d[- ]?){12}\d$/;

/** Mirrors the API's validation, so most mistakes are caught before a request is sent. */
const schema = z.object({
  isbn: z.string().trim().regex(ISBN, 'Enter a 10- or 13-digit ISBN'),
  title: z.string().trim().min(1, 'Enter a title').max(200, 'At most 200 characters'),
  author: z.string().trim().min(1, 'Enter an author').max(150, 'At most 150 characters'),
  category: z
    .string()
    .trim()
    .max(50, 'At most 50 characters')
    .transform((value) => value || null),
  publishedYear: z
    .string()
    .trim()
    .refine((value) => value === '' || (/^\d{4}$/.test(value) && +value >= 1450 && +value <= 2100), {
      message: 'Enter a year between 1450 and 2100',
    })
    .transform((value) => (value ? Number(value) : null)),
  totalCopies: z
    .string()
    .trim()
    .regex(/^\d+$/, 'Enter a whole number')
    .transform(Number)
    .refine((value) => value <= 1000, { message: 'At most 1000 copies' }),
});

type FormValues = z.input<typeof schema>;
type FormFields = keyof FormValues;

function toFormValues(book?: Book): FormValues {
  return {
    isbn: book?.isbn ?? '',
    title: book?.title ?? '',
    author: book?.author ?? '',
    category: book?.category ?? '',
    publishedYear: book?.publishedYear?.toString() ?? '',
    totalCopies: book?.totalCopies.toString() ?? '1',
  };
}

interface BookFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The book to edit; omit to add a new one. */
  book?: Book;
  onSaved?: (book: Book) => void;
}

export function BookFormDialog({ open, onOpenChange, book, onSaved }: BookFormDialogProps) {
  return (
    <Modal open={open} onOpenChange={onOpenChange} title={book ? 'Edit book' : 'Add book'}>
      {/* Mounted only while open, so every opening starts from the latest values. */}
      <BookForm
        book={book}
        onCancel={() => onOpenChange(false)}
        onSaved={(saved) => {
          onOpenChange(false);
          onSaved?.(saved);
        }}
      />
    </Modal>
  );
}

function BookForm({
  book,
  onCancel,
  onSaved,
}: {
  book?: Book;
  onCancel: () => void;
  onSaved: (book: Book) => void;
}) {
  const queryClient = useQueryClient();
  // The version this edit is based on; replaced when the user reloads after a conflict.
  const [version, setVersion] = useState(book?.version);
  const [reloaded, setReloaded] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues, unknown, z.output<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: toFormValues(book),
  });

  const save = useMutation({
    mutationFn: (input: BookInput) => (book ? updateBook(book.id, { ...input, version }) : createBook(input)),
    onSuccess: (saved) => {
      queryClient.setQueryData(queryKeys.book(saved.id), saved);
      void queryClient.invalidateQueries({ queryKey: queryKeys.books });
      onSaved(saved);
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          setError(field as FormFields, { message }),
        );
      }
    },
  });

  const reload = useMutation({
    mutationFn: () =>
      queryClient.fetchQuery({
        queryKey: queryKeys.book(book!.id),
        queryFn: () => getBook(book!.id),
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
          Loaded the latest version of this book. Your unsaved changes were replaced; edit and save again.
        </p>
      )}
      <FormField label="ISBN" error={errors.isbn?.message} {...register('isbn')} />
      <FormField label="Title" error={errors.title?.message} {...register('title')} />
      <FormField label="Author" error={errors.author?.message} {...register('author')} />
      <FormField label="Category" error={errors.category?.message} {...register('category')} />
      <FormField
        label="Published year"
        inputMode="numeric"
        error={errors.publishedYear?.message}
        {...register('publishedYear')}
      />
      <FormField
        label="Total copies"
        inputMode="numeric"
        error={errors.totalCopies?.message}
        {...register('totalCopies')}
      />
      <div className="actions">
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={save.isPending || reload.isPending}>
          {save.isPending ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  );
}
