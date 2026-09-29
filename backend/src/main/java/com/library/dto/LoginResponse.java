package com.library.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, String fullName) {
}
