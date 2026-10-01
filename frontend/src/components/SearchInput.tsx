import { useEffect, useRef, useState } from 'react';
import styles from './SearchInput.module.css';

interface SearchInputProps {
  label: string;
  /** The current search, e.g. from the URL. */
  value: string;
  /** Called with the trimmed text once typing pauses. */
  onSearch: (value: string) => void;
  placeholder?: string;
  delayMs?: number;
}

export function SearchInput({ label, value, onSearch, placeholder, delayMs = 300 }: SearchInputProps) {
  const [text, setText] = useState(value);
  const [shownValue, setShownValue] = useState(value);
  const timer = useRef<number | undefined>(undefined);

  // Follow outside changes (e.g. the Back button changing the URL) without an effect.
  if (value !== shownValue) {
    setShownValue(value);
    setText(value);
  }

  useEffect(() => () => window.clearTimeout(timer.current), []);

  function handleChange(next: string) {
    setText(next);
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => onSearch(next.trim()), delayMs);
  }

  return (
    <label className={styles.search}>
      <span className={styles.label}>{label}</span>
      <input
        type="search"
        className={styles.input}
        value={text}
        placeholder={placeholder}
        onChange={(event) => handleChange(event.target.value)}
      />
    </label>
  );
}
