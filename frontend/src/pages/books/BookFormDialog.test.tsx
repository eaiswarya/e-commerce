import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createBook, getBook, updateBook } from '../../api/books';
import { ApiError } from '../../api/client';
import { renderWithProviders } from '../../test/render';
import type { Book } from '../../types';
import { BookFormDialog } from './BookFormDialog';

vi.mock('../../api/books', () => ({ createBook: vi.fn(), updateBook: vi.fn(), getBook: vi.fn() }));

const BOOK: Book = {
  id: 7,
  isbn: '9780134685991',
  title: 'Effective Java',
  author: 'Joshua Bloch',
  category: 'Programming',
  publishedYear: 2018,
  totalCopies: 3,
  availableCopies: 2,
  version: 4,
};

function renderDialog(book?: Book) {
  const onOpenChange = vi.fn();
  const onSaved = vi.fn();
  renderWithProviders(<BookFormDialog open onOpenChange={onOpenChange} book={book} onSaved={onSaved} />);
  return { onOpenChange, onSaved };
}

async function fill(label: string, value: string) {
  const input = screen.getByLabelText(label);
  await userEvent.clear(input);
  if (value) await userEvent.type(input, value);
}

const save = () => userEvent.click(screen.getByRole('button', { name: 'Save' }));

beforeEach(() => {
  vi.mocked(createBook).mockReset();
  vi.mocked(updateBook).mockReset();
  vi.mocked(getBook).mockReset();
});

describe('BookFormDialog', () => {
  it('checks the fields before sending anything', async () => {
    renderDialog();

    await fill('ISBN', '12345');
    await fill('Published year', '99');
    await fill('Total copies', 'two');
    await save();

    expect(screen.getByLabelText('ISBN')).toHaveAccessibleDescription('Enter a 10- or 13-digit ISBN');
    expect(screen.getByLabelText('Title')).toHaveAccessibleDescription('Enter a title');
    expect(screen.getByLabelText('Author')).toHaveAccessibleDescription('Enter an author');
    expect(screen.getByLabelText('Published year')).toHaveAccessibleDescription(
      'Enter a year between 1450 and 2100',
    );
    expect(screen.getByLabelText('Total copies')).toHaveAccessibleDescription('Enter a whole number');
    expect(createBook).not.toHaveBeenCalled();
  });

  it('adds a book, sending empty optional fields as null', async () => {
    vi.mocked(createBook).mockResolvedValue(BOOK);
    const { onOpenChange, onSaved } = renderDialog();

    expect(screen.getByRole('dialog', { name: 'Add book' })).toBeInTheDocument();
    await fill('ISBN', ' 978-0-13-468599-1 ');
    await fill('Title', 'Effective Java');
    await fill('Author', 'Joshua Bloch');
    await fill('Total copies', '2');
    await save();

    expect(createBook).toHaveBeenCalledWith({
      isbn: '978-0-13-468599-1',
      title: 'Effective Java',
      author: 'Joshua Bloch',
      category: null,
      publishedYear: null,
      totalCopies: 2,
    });
    expect(onSaved).toHaveBeenCalledWith(BOOK);
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(await screen.findByText('“Effective Java” added to the catalogue')).toBeInTheDocument();
  });

  it('edits a book, sending the version it was loaded with', async () => {
    vi.mocked(updateBook).mockResolvedValue({ ...BOOK, title: 'Effective Java 3', version: 5 });
    renderDialog(BOOK);

    expect(screen.getByLabelText('Title')).toHaveValue('Effective Java');
    await fill('Title', 'Effective Java 3');
    await save();

    expect(updateBook).toHaveBeenCalledWith(7, {
      isbn: '9780134685991',
      title: 'Effective Java 3',
      author: 'Joshua Bloch',
      category: 'Programming',
      publishedYear: 2018,
      totalCopies: 3,
      version: 4,
    });
    expect(await screen.findByText('Changes saved')).toBeInTheDocument();
  });

  it('shows the API field errors on their fields', async () => {
    vi.mocked(updateBook).mockRejectedValue(
      new ApiError(400, 'VALIDATION_FAILED', 'Request validation failed', { title: 'must not be blank' }),
    );
    renderDialog(BOOK);

    await save();

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
  });

  it('marks the ISBN when another book already has it', async () => {
    vi.mocked(updateBook).mockRejectedValue(
      new ApiError(409, 'DUPLICATE', 'A book with ISBN 9780134685991 already exists'),
    );
    renderDialog(BOOK);

    await save();

    expect(await screen.findByLabelText('ISBN')).toHaveAccessibleDescription(
      'A book with this ISBN already exists',
    );
  });

  it('explains a rule the API enforces, such as copies on loan', async () => {
    vi.mocked(updateBook).mockRejectedValue(
      new ApiError(409, 'COPIES_ON_LOAN', 'Cannot set total copies to 0'),
    );
    renderDialog(BOOK);

    await fill('Total copies', '0');
    await save();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Total copies cannot be lower than the number of copies currently on loan.',
    );
    expect(screen.queryByRole('button', { name: 'Reload latest' })).not.toBeInTheDocument();
  });

  it('after someone else changed the book, reloads the latest version and saves with it', async () => {
    vi.mocked(updateBook)
      .mockRejectedValueOnce(
        new ApiError(409, 'CONCURRENT_UPDATE', 'The record was changed by someone else.'),
      )
      .mockResolvedValueOnce({ ...BOOK, title: 'Mine after all', version: 6 });
    vi.mocked(getBook).mockResolvedValue({ ...BOOK, title: 'Changed elsewhere', version: 5 });
    renderDialog(BOOK);

    await fill('Title', 'My edit');
    await save();
    expect(await screen.findByRole('alert')).toHaveTextContent('Someone else changed this record');
    await userEvent.click(screen.getByRole('button', { name: 'Reload latest' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Loaded the latest version of this book');
    expect(screen.getByLabelText('Title')).toHaveValue('Changed elsewhere');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await fill('Title', 'Mine after all');
    await save();
    expect(updateBook).toHaveBeenLastCalledWith(
      7,
      expect.objectContaining({ title: 'Mine after all', version: 5 }),
    );
  });
});
