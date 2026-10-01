import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DataTable } from './DataTable';

describe('DataTable', () => {
  it('renders a header per column and a row per item', () => {
    render(
      <DataTable
        caption="Books"
        rows={[
          { id: 1, title: 'Dune' },
          { id: 2, title: 'Emma' },
        ]}
        rowKey={(row) => row.id}
        columns={[
          { header: 'ID', cell: (row) => row.id },
          { header: 'Title', cell: (row) => <strong>{row.title}</strong> },
        ]}
      />,
    );

    const table = screen.getByRole('table', { name: 'Books' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((th) => th.textContent),
    ).toEqual(['ID', 'Title']);
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows.map((row) => row.textContent)).toEqual(['1Dune', '2Emma']);
  });
});
