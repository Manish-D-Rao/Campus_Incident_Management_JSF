package com.campus.incident.security;

import com.campus.incident.entity.User;
import com.campus.incident.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JwtAuthFilter is a Spring Security filter that runs on every HTTP request.
 * It extends OncePerRequestFilter so it executes exactly once per request.
 *
 * Purpose:
 *   - Intercept incoming requests that have an "Authorization: Bearer <token>" header
 *   - Extract the JWT token from the header
 *   - Validate the token using JwtUtil
 *   - If valid, load the user from the database and set up Spring Security's
 *     SecurityContext so the request is treated as authenticated
 *   - If no token or invalid token, let the request pass through unauthenticated
 *     (Spring Security will reject it later if the endpoint requires authentication)
 *
 * This makes the application STATELESS: the server does not store sessions.
 * The client must send the JWT on every request.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Core filter logic executed for every request.
     *
     * @param request    the incoming HTTP request
     * @param response   the HTTP response
     * @param filterChain the chain to pass the request to the next filter / controller
     * @throws ServletException, IOException
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Read the "Authorization" header from the request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Step 2: If no Authorization header, or it doesn't start with "Bearer ",
        // there is no JWT to process. Let the request continue unauthenticated.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the token by removing the "Bearer " prefix (7 characters)
        jwt = authHeader.substring(7);

        // Step 4: Extract the username (email) embedded in the JWT token
        userEmail = jwtUtil.extractUsername(jwt);

        // Step 5: If we got a username AND no authentication exists in the context yet,
        // proceed to validate the token and load the user
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Look up the user in the database by email
            Optional<User> userDetails = userRepository.findByEmail(userEmail);

            // If user exists AND the token is valid (signature OK + not expired)
            if (userDetails.isPresent() && jwtUtil.isTokenValid(jwt, new org.springframework.security.core.userdetails.User(
                    userDetails.get().getEmail(),
                    userDetails.get().getPassword(),
                    java.util.Collections.emptyList()
            ))) {

                // Step 6: Build Spring Security authentication token
                // - principal: the User entity object
                // - credentials: null (we already validated via JWT, no password needed here)
                // - authorities: a single role like "ROLE_ADMIN", "ROLE_OFFICER", etc.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails.get(),
                        null,
                        userDetails.get().getAuthorities()
                );

                // Attach request details (remote address, etc.) for auditing
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 7: Store the authentication in Spring Security's context.
                // After this, the request is considered authenticated for the rest of the filter chain.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 8: Always continue the filter chain (pass request to next filter or controller)
        filterChain.doFilter(request, response);
    }
}
