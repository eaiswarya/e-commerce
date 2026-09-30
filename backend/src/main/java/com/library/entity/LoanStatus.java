package com.library.entity;

/**
 * A loan is {@link #ACTIVE} until returned, {@link #OVERDUE} while active past its due date, then
 * {@link #RETURNED}. {@link #ALL} is only a search filter meaning "any status".
 */
public enum LoanStatus {
	ACTIVE, OVERDUE, RETURNED, ALL
}
