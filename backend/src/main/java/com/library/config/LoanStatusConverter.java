package com.library.config;

import com.library.entity.LoanStatus;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets {@code ?status=} be written in any case ({@code active}, {@code OVERDUE}, ...). An unknown value fails
 * conversion, which {@code GlobalExceptionHandler} answers with 400 {@code BAD_REQUEST}.
 */
@Component
public class LoanStatusConverter implements Converter<String, LoanStatus> {

	@Override
	public LoanStatus convert(String source) {
		String value = source.trim();
		return value.isEmpty() ? null : LoanStatus.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
