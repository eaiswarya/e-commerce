import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { deleteBook, getBook } from '../../api/books';
import { queryKeys } from '../../api/queryKeys';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { QueryState } from '../../components/QueryState';
import { BookFormDialog } from './BookFormDialog';

export function BookDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const query = useQuery({ queryKey: queryKeys.book(id), queryFn: ({ signal }) => getBook(id, signal) });
  const remove = useMutation({
    mutationFn: () => deleteBook(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: queryKeys.book(id) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.bookSearches });
      navigate('/books', { replace: true });
    },
  });

  return (
    <section>
      <p>
        <Link to="/books">← All books</Link>
      </p>
      <QueryState
        query={query}
        notFound={
          <EmptyState
            message="Book not found. It may have been deleted."
            action={<Link to="/books">Back to books</Link>}
          />
        }
      >
        {(book) => (
          <>
            <div className="page-header">
              <h1>{book.title}</h1>
              <div className="header-actions">
                <button type="button" className="btn" onClick={() => setEditing(true)}>
                  Edit
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => {
                    remove.reset();
                    setDeleting(true);
                  }}
                >
                  Delete
                </button>
              </div>
            </div>
            <dl className="details">
              <dt>Author</dt>
              <dd>{book.author}</dd>
              <dt>ISBN</dt>
              <dd>{book.isbn}</dd>
              <dt>Category</dt>
              <dd>{book.category ?? '—'}</dd>
              <dt>Published</dt>
              <dd>{book.publishedYear ?? '—'}</dd>
              <dt>Copies available</dt>
              <dd>
                {book.availableCopies} of {book.totalCopies}
              </dd>
            </dl>
            <BookFormDialog open={editing} onOpenChange={setEditing} book={book} />
            <ConfirmDialog
              open={deleting}
              onOpenChange={setDeleting}
              title="Delete book?"
              confirmLabel="Delete"
              onConfirm={() => remove.mutate()}
              isPending={remove.isPending}
              error={remove.error}
            >
              <p>“{book.title}” will be removed from the catalogue. This cannot be undone.</p>
            </ConfirmDialog>
          </>
        )}
      </QueryState>
    </section>
  );
}
