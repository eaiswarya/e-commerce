package com.library.repository;

import com.library.entity.Member;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Search filters for members. A blank or missing value means "no filter" and returns
 * {@link Specification#unrestricted()}, so filters can always be combined with {@code allOf}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberSpecifications {

	private static final char ESCAPE = '\\';

	/** Full name or email contains {@code q} (ignoring case), or the member code equals {@code q} (ignoring case). */
	public static Specification<Member> matchesQuery(String q) {
		if (!StringUtils.hasText(q)) {
			return Specification.unrestricted();
		}
		String value = q.trim().toLowerCase(Locale.ROOT);
		String pattern = "%" + escapeLike(value) + "%";
		return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("fullName")), pattern, ESCAPE),
				cb.like(cb.lower(root.get("email")), pattern, ESCAPE),
				cb.equal(cb.lower(root.get("memberCode")), value));
	}

	/** {@code true} or {@code false} keeps members with that status; {@code null} applies no filter. */
	public static Specification<Member> hasActive(Boolean active) {
		if (active == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("active"), active);
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
