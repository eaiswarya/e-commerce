import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { searchBooks, type BookSearch } from '../../api/books';
import { queryKeys } from '../../api/queryKeys';
import { DataTable, type Column } from '../../components/DataTable';
import { EmptyState } from '../../components/EmptyState';
import { Pagination } from '../../components/Pagination';
import { QueryState } from '../../components/QueryState';
import { SearchInput } from '../../components/SearchInput';
import type { Book } from '../../types';
import { useListParams } from '../useListParams';
import { BookFormDialog } from './BookFormDialog';

const columns: Column<Book>[] = [
  { header: 'Title', cell: (book) => <Link to={`/books/${book.id}`}>{book.title}</Link> },
  { header: 'Author', cell: (book) => book.author },
  { header: 'ISBN', cell: (book) => book.isbn },
  { header: 'Category', cell: (book) => book.category ?? '—' },
  { header: 'Available', cell: (book) => `${book.availableCopies} of ${book.totalCopies}` },
];

export function BooksPage() {
  const { params, page: pageNumber, update, setPage } = useListParams();
  const search: BookSearch = {
    q: params.get('q') ?? '',
    category: params.get('category') ?? '',
    available: params.get('available') === 'true',
    page: pageNumber,
  };
  const [adding, setAdding] = useState(false);
  const query = useQuery({
    queryKey: queryKeys.bookSearch(search),
    queryFn: ({ signal }) => searchBooks(search, signal),
    placeholderData: keepPreviousData,
  });

  const filtered = Boolean(search.q || search.category || search.available);

  return (
    <section>
      <div className="page-header">
        <h1>Books</h1>
        <button type="button" className="btn btn-primary" onClick={() => setAdding(true)}>
          Add book
        </button>
      </div>
      <div className="toolbar">
        <SearchInput
          label="Search"
          placeholder="Title, author or ISBN"
          value={search.q ?? ''}
          onSearch={(q) => update({ q })}
        />
        <SearchInput
          label="Category"
          value={search.category ?? ''}
          onSearch={(category) => update({ category })}
        />
        <label>
          <input
            type="checkbox"
            checked={search.available}
            onChange={(event) => update({ available: event.target.checked ? 'true' : null })}
          />{' '}
          Available only
        </label>
      </div>
      <QueryState
        query={query}
        isEmpty={(page) => page.content.length === 0}
        empty={
          <EmptyState
            message={filtered ? 'No books match your search.' : 'No books yet. Add the first one.'}
          />
        }
      >
        {(page) => (
          <>
            <DataTable caption="Books" columns={columns} rows={page.content} rowKey={(book) => book.id} />
            <Pagination page={page.page} totalPages={page.totalPages} onChange={setPage} />
          </>
        )}
      </QueryState>
      <BookFormDialog open={adding} onOpenChange={setAdding} />
    </section>
  );
}
