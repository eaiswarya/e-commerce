import { keepPreviousData, useQuery, type QueryKey } from '@tanstack/react-query';
import { useState } from 'react';
import type { PageResponse } from '../../types';
import { QueryState } from '../QueryState';
import { SearchInput } from '../SearchInput';
import styles from './Picker.module.css';

interface PickerProps<T extends { id: number }> {
  legend: string;
  searchLabel: string;
  queryKey: (q: string) => QueryKey;
  search: (q: string, signal: AbortSignal) => Promise<PageResponse<T>>;
  label: (item: T) => string;
  hint: (item: T) => string;
  selected: T | null;
  onSelect: (item: T) => void;
  emptyText: string;
}

/** Search box plus a radio list of the first matches; the chosen item stays selected while the search changes. */
export function Picker<T extends { id: number }>({
  legend,
  searchLabel,
  queryKey,
  search,
  label,
  hint,
  selected,
  onSelect,
  emptyText,
}: PickerProps<T>) {
  const [q, setQ] = useState('');
  const query = useQuery({
    queryKey: queryKey(q),
    queryFn: ({ signal }) => search(q, signal),
    placeholderData: keepPreviousData,
  });

  return (
    <fieldset className={styles.picker}>
      <legend className={styles.legend}>{legend}</legend>
      <SearchInput label={searchLabel} value={q} onSearch={setQ} />
      <QueryState
        query={query}
        isEmpty={(page) => page.content.length === 0}
        empty={<p className={styles.empty}>{emptyText}</p>}
      >
        {(page) => (
          <ul className={styles.options}>
            {page.content.map((item) => (
              <li key={item.id}>
                <label className={styles.option}>
                  <input
                    type="radio"
                    name={legend}
                    checked={selected?.id === item.id}
                    onChange={() => onSelect(item)}
                  />
                  <span>
                    {label(item)}
                    <span className={styles.hint}>{hint(item)}</span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
        )}
      </QueryState>
    </fieldset>
  );
}
