package com.campus.incident.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JwtUtil is a helper class for JWT (JSON Web Token) operations.
 * It handles:
 *   1. Generating a JWT token after successful login
 *   2. Extracting claims (username, expiration) from a token
 *   3. Validating whether a token is still valid and not expired
 *
 * JWT flow in this project:
 *   - Client sends username+password to /api/auth/login
 *   - Server validates credentials, then JwtUtil.generateToken() creates a signed JWT
 *   - Server returns the JWT to the client
 *   - Client stores the JWT (e.g., in localStorage) and sends it on every request
 *   - Header format: Authorization: Bearer <token>
 *   - JwtAuthFilter extracts the token and calls JwtUtil to validate it
 */
@Component
public class JwtUtil {

    // Secret key used to sign and verify JWT tokens.
    // Injected from application.properties: app.jwt.secret
    // Must be kept secret; if leaked, anyone can forge tokens.
    @Value("${app.jwt.secret}")
    private String secretKey;

    // Token expiration time in milliseconds.
    // Default 86400000 ms = 24 hours. Injected from app.jwt.expiration.
    @Value("${app.jwt.expiration:86400000}")
    private long expiration;

    /**
     * Creates the cryptographic signing key from the secret string.
     * Uses HMAC-SHA algorithm (Keys.hmacShaKeyFor).
     *
     * NOTE: We use secretKey.getBytes() directly (raw bytes) instead of
     * Base64 decoding, because our secret key in application.properties is
     * a plain string (not Base64-encoded). Using Base64 decode on a plain
     * string with hyphens caused "Illegal base64 character" error.
     *
     * @return SecretKey used to sign/verify JWT signatures
     */
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Extracts the username (subject) from the JWT token.
     * The subject was set to the user's email during token generation.
     *
     * @param token the JWT token string
     * @return the username (email) embedded in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT token string
     * @return the Date when the token expires
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from the token.
     * Parses the token, then applies the claimsResolver function to get the desired claim.
     *
     * @param token          the JWT token string
     * @param claimsResolver function to extract a specific claim type from Claims
     * @param <T>            the type of the claim being extracted
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT token and returns the Claims body (payload).
     * Verifies the token signature using the signing key.
     * If the token is invalid or tampered with, this throws an exception.
     *
     * @param token the JWT token string
     * @return the Claims object containing token payload data
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())   // verify signature with our secret key
                .build()
                .parseSignedClaims(token)     // parse and validate the token
                .getPayload();                // return the claims (payload) part
    }

    /**
     * Generates a new JWT token for an authenticated user.
     * The token contains:
     *   - subject (sub): the user's email (used as username)
     *   - issued at (iat): current timestamp
     *   - expiration (exp): current time + expiration duration
     *
     * @param userDetails the authenticated user's details (contains username/email)
     * @return the signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())          // set "sub" claim to user email
                .issuedAt(new Date(System.currentTimeMillis())) // set "iat" claim to now
                .expiration(new Date(System.currentTimeMillis() + expiration)) // set "exp" claim
                .signWith(getSignInKey())                    // sign with HMAC key
                .compact();                                  // build the final token string
    }

    /**
     * Validates whether a JWT token is acceptable for a given user.
     * Checks two conditions:
     *   1. The username in the token matches the expected userDetails username
     *   2. The token has not expired
     *
     * @param token       the JWT token to validate
     * @param userDetails the expected user details (to compare username)
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if the token's expiration date is before the current time.
     *
     * @param token the JWT token string
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
