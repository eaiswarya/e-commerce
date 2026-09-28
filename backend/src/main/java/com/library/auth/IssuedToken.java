package com.library.auth;

import java.time.Instant;

public record IssuedToken(String token, Instant expiresAt) {
}
