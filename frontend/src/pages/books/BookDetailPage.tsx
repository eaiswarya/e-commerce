import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookUp, ChevronLeft, Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { deleteBook, getBook } from '../../api/books';
import { queryKeys } from '../../api/queryKeys';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { BorrowDialog } from '../../components/loans/BorrowDialog';
import { QueryState } from '../../components/QueryState';
import { BookFormDialog } from './BookFormDialog';
import { BookLoans } from './BookLoans';

export function BookDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [lending, setLending] = useState(false);
  const query = useQuery({ queryKey: queryKeys.book(id), queryFn: ({ signal }) => getBook(id, signal) });
  const remove = useMutation({
    mutationFn: () => deleteBook(id),
    onSuccess: () => {
      toast.success(`“${query.data?.title}” deleted`);
      queryClient.removeQueries({ queryKey: queryKeys.book(id) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.bookSearches });
      navigate('/books', { replace: true });
    },
  });

  return (
    <section>
      <Link to="/books" className="back-link">
        <ChevronLeft size={16} /> All books
      </Link>
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
              <div>
                <h1>{book.title}</h1>
                <p className="page-subtitle">by {book.author}</p>
              </div>
              <div className="header-actions">
                {book.availableCopies > 0 && (
                  <button type="button" className="btn btn-primary" onClick={() => setLending(true)}>
                    <BookUp />
                    Lend this book
                  </button>
                )}
                <button type="button" className="btn" onClick={() => setEditing(true)}>
                  <Pencil />
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
                  <Trash2 />
                  Delete
                </button>
              </div>
            </div>
            <dl className="details card">
              <div>
                <dt>Author</dt>
                <dd>{book.author}</dd>
              </div>
              <div>
                <dt>ISBN</dt>
                <dd>{book.isbn}</dd>
              </div>
              <div>
                <dt>Category</dt>
                <dd>{book.category ?? '—'}</dd>
              </div>
              <div>
                <dt>Published</dt>
                <dd>{book.publishedYear ?? '—'}</dd>
              </div>
              <div>
                <dt>Copies available</dt>
                <dd>
                  {book.availableCopies} of {book.totalCopies}
                </dd>
              </div>
            </dl>
            <BookLoans bookId={book.id} />
            <BookFormDialog open={editing} onOpenChange={setEditing} book={book} />
            <BorrowDialog open={lending} onOpenChange={setLending} book={book} />
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
