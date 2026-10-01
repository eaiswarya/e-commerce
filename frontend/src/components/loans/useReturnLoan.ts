import { useMutation, useQueryClient } from '@tanstack/react-query';
import { returnLoan } from '../../api/loans';
import { queryKeys } from '../../api/queryKeys';

/** Takes a book back; refreshes every loan list and the book counts, which both change. */
export function useReturnLoan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (loanId: number) => returnLoan(loanId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.loans });
      void queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}
