import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { returnLoan } from '../../api/loans';
import { queryKeys } from '../../api/queryKeys';

/** Takes a book back; refreshes every loan list and the book counts, which both change. */
export function useReturnLoan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (loanId: number) => returnLoan(loanId),
    onSuccess: (loan) => {
      toast.success(`“${loan.bookTitle}” returned`);
      void queryClient.invalidateQueries({ queryKey: queryKeys.loans });
      void queryClient.invalidateQueries({ queryKey: queryKeys.books });
    },
  });
}
