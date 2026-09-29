package com.library.entity;

import com.library.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import java.util.Locale;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 13)
	private String isbn;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 150)
	private String author;

	@Column(length = 50)
	private String category;

	@Column(name = "published_year")
	private Integer publishedYear;

	@Column(name = "total_copies", nullable = false)
	private int totalCopies;

	@Column(name = "available_copies", nullable = false)
	private int availableCopies;

	@Version
	@Column(nullable = false)
	private Long version;

	public Book(String isbn, String title, String author, String category, Integer publishedYear, int totalCopies) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.category = category;
		this.publishedYear = publishedYear;
		this.totalCopies = totalCopies;
		this.availableCopies = totalCopies;
	}

	/** Strips hyphens and whitespace and uppercases a trailing ISBN-10 check digit {@code x}. */
	public static String normaliseIsbn(String isbn) {
		return (isbn == null) ? null : isbn.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
	}

	public void updateDetails(String isbn, String title, String author, String category, Integer publishedYear) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.category = category;
		this.publishedYear = publishedYear;
	}

	public void changeTotalCopies(int newTotal) {
		int onLoan = getCopiesOnLoan();
		if (newTotal < onLoan) {
			throw new BusinessRuleException("COPIES_ON_LOAN",
				"Cannot set total copies to %d: %d copies are on loan".formatted(newTotal, onLoan));
		}
		this.totalCopies = newTotal;
		this.availableCopies = newTotal - onLoan;
	}

	public int getCopiesOnLoan() {
		return totalCopies - availableCopies;
	}

	public boolean hasCopiesOnLoan() {
		return getCopiesOnLoan() > 0;
	}
}
