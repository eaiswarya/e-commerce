package com.library.repository;

import com.library.entity.Book;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Search filters for books. A blank or missing value means "no filter" and returns
 * {@link Specification#unrestricted()}, so filters can always be combined with {@code allOf}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookSpecifications {

	private static final char ESCAPE = '\\';

	/** Title or author contains {@code q} (ignoring case), or the ISBN equals {@code q} once normalised. */
	public static Specification<Book> matchesQuery(String q) {
		if (!StringUtils.hasText(q)) {
			return Specification.unrestricted();
		}
		String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
		String isbn = Book.normaliseIsbn(q);
		return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
				cb.like(cb.lower(root.get("author")), pattern, ESCAPE), cb.equal(root.get("isbn"), isbn));
	}

	public static Specification<Book> hasCategory(String category) {
		if (!StringUtils.hasText(category)) {
			return Specification.unrestricted();
		}
		String value = category.trim().toLowerCase(Locale.ROOT);
		return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), value);
	}

	/** {@code true} keeps only books with a free copy; {@code false} or {@code null} applies no filter. */
	public static Specification<Book> isAvailable(Boolean available) {
		if (!Boolean.TRUE.equals(available)) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.greaterThan(root.get("availableCopies"), 0);
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
