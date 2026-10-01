import { useSearchParams } from 'react-router';

/**
 * A list page's filters and page number, kept in the URL so reload and Back keep them. {@code update} applies
 * changes (null or '' removes a value); any change other than the page itself goes back to the first page.
 */
export function useListParams() {
  const [params, setParams] = useSearchParams();
  const parsed = Number.parseInt(params.get('page') ?? '', 10);
  // A hand-edited or stale URL like ?page=abc or ?page=-1 just shows the first page.
  const page = Number.isInteger(parsed) && parsed > 0 ? parsed : 0;

  function update(changes: Record<string, string | null>) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      for (const [key, value] of Object.entries(changes)) {
        if (value) next.set(key, value);
        else next.delete(key);
      }
      if (!('page' in changes)) next.delete('page');
      return next;
    });
  }

  function setPage(next: number) {
    update({ page: next > 0 ? String(next) : null });
  }

  return { params, page, update, setPage };
}
