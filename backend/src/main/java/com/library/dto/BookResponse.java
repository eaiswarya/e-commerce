package com.library.dto;

import com.library.entity.Book;

public record BookResponse(Long id, String isbn, String title, String author, String category, Integer publishedYear,
		int totalCopies, int availableCopies) {

	public static BookResponse from(Book book) {
		return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCategory(),
				book.getPublishedYear(), book.getTotalCopies(), book.getAvailableCopies());
	}
}
