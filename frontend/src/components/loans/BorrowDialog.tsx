import { useMutation, useQueryClient } from '@tanstack/react-query';
import { BookUp } from 'lucide-react';
import { useState } from 'react';
import { toast } from 'sonner';
import { searchBooks } from '../../api/books';
import { errorMessage } from '../../api/errors';
import { borrowBook } from '../../api/loans';
import { searchMembers } from '../../api/members';
import { queryKeys } from '../../api/queryKeys';
import type { Book, Member } from '../../types';
import { formatDate } from '../../utils/dates';
import { ErrorBanner } from '../ErrorBanner';
import { Modal } from '../Modal';
import { Picker } from './Picker';

const PICKER_SIZE = 10;

interface BorrowDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Start with this member chosen, e.g. when lending from the member's page. */
  member?: Member;
  /** Start with this book chosen, e.g. when lending from the book's page. */
  book?: Book;
}

export function BorrowDialog({ open, onOpenChange, member, book }: BorrowDialogProps) {
  return (
    <Modal open={open} onOpenChange={onOpenChange} title="Lend a book">
      {/* Mounted only while open, so each lending starts from a clean form. */}
      <BorrowForm initialMember={member} initialBook={book} onDone={() => onOpenChange(false)} />
    </Modal>
  );
}

function BorrowForm({
  initialMember,
  initialBook,
  onDone,
}: {
  initialMember?: Member;
  initialBook?: Book;
  onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const [member, setMember] = useState<Member | null>(initialMember ?? null);
  const [book, setBook] = useState<Book | null>(initialBook ?? null);
  const lend = useMutation({
    mutationFn: ({ bookId, memberId }: { bookId: number; memberId: number }) => borrowBook(bookId, memberId),
    onSuccess: (loan) => {
      toast.success(`“${loan.bookTitle}” lent to ${loan.memberName}`, {
        description: `Due back ${formatDate(loan.dueDate)}`,
      });
      void queryClient.invalidateQueries({ queryKey: queryKeys.loans });
      void queryClient.invalidateQueries({ queryKey: queryKeys.books });
      onDone();
    },
  });

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        if (member && book) lend.mutate({ bookId: book.id, memberId: member.id });
      }}
    >
      {lend.isError && <ErrorBanner message={errorMessage(lend.error)} />}
      {initialMember ? (
        <p>
          Member: <strong>{initialMember.fullName}</strong> ({initialMember.memberCode})
        </p>
      ) : (
        <Picker<Member>
          legend="Member"
          searchLabel="Find member"
          queryKey={(q) => queryKeys.memberSearch({ q, active: true, size: PICKER_SIZE })}
          search={(q, signal) => searchMembers({ q, active: true, size: PICKER_SIZE }, signal)}
          label={(m) => `${m.fullName} (${m.memberCode})`}
          hint={(m) => m.email}
          selected={member}
          onSelect={setMember}
          emptyText="No active members match."
        />
      )}
      {initialBook ? (
        <p>
          Book: <strong>{initialBook.title}</strong> by {initialBook.author}
        </p>
      ) : (
        <Picker<Book>
          legend="Book"
          searchLabel="Find book"
          queryKey={(q) => queryKeys.bookSearch({ q, available: true, size: PICKER_SIZE })}
          search={(q, signal) => searchBooks({ q, available: true, size: PICKER_SIZE }, signal)}
          label={(b) => `${b.title} by ${b.author}`}
          hint={(b) => `${b.availableCopies} of ${b.totalCopies} available`}
          selected={book}
          onSelect={setBook}
          emptyText="No available books match."
        />
      )}
      {member && book && (
        <p role="status">
          Lend <strong>{book.title}</strong> to <strong>{member.fullName}</strong>.
        </p>
      )}
      <div className="actions">
        <button type="button" className="btn" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={!member || !book || lend.isPending}>
          <BookUp />
          {lend.isPending ? 'Lending…' : 'Lend book'}
        </button>
      </div>
    </form>
  );
}
