package com.coffeehut.coffeehut.oneMenu;

import com.coffeehut.coffeehut.model.StaffAccount;
import com.coffeehut.coffeehut.repository.StaffAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * <p>
 * Covers requirements: NFR6, NFR7.
 * All dependencies are mocked via Mockito — no real database is used.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StaffAccountRepository staffAccountRepository;

    @InjectMocks
    private AuthService authService;

    private StaffAccount staffAccount;

    @BeforeEach
    void setUp() {
        staffAccount = new StaffAccount();
        staffAccount.setId(1L);
        staffAccount.setName("Admin");
        staffAccount.setEmail("admin@coffeehut.com");
        staffAccount.setPassword("secret123");
    }

    // ─── NFR6: Security — staff portal access restrictions ──────────────────

    /**
     * Verifies that {@code login()} returns HTTP 200 and staff identity
     * when valid credentials are supplied.
     * <p>
     * Only staff accounts stored in {@code staff_accounts} are accepted;
     * customer loyalty accounts must not grant staff portal access.
     * </p>
     *
     * @throws AssertionError if the response status is not 200 or body is missing fields
     * Covers NFR6
     */
    @Test
    void login_withValidCredentials_returnsOkWithIdentity() {
        when(staffAccountRepository.findByEmail("admin@coffeehut.com"))
                .thenReturn(Optional.of(staffAccount));

        ResponseEntity<?> response = authService.login("admin@coffeehut.com", "secret123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("memberId");
        assertThat(body).containsKey("name");
        assertThat(body.get("name")).isEqualTo("Admin");
    }

    /**
     * Verifies that {@code login()} returns HTTP 401 when the password
     * does not match the stored value.
     * <p>
     * This guards against unauthorised staff portal access. Until password
     * hashing is implemented, a plain-text comparison is performed; the
     * HTTP status must still be 401 on mismatch (NFR6).
     * </p>
     *
     * @throws AssertionError if the response status is not 401
     * Covers NFR6, NFR7
     */
    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        when(staffAccountRepository.findByEmail("admin@coffeehut.com"))
                .thenReturn(Optional.of(staffAccount));

        ResponseEntity<?> response = authService.login("admin@coffeehut.com", "wrongpassword");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
    }

    /**
     * Verifies that {@code login()} returns HTTP 401 when the email address
     * does not match any staff account.
     * <p>
     * Customer accounts and unregistered emails must be denied access to
     * the staff portal entirely (NFR6).
     * </p>
     *
     * @throws AssertionError if the response status is not 401
     * Covers NFR6, NFR7
     */
    @Test
    void login_withUnknownEmail_returnsUnauthorized() {
        when(staffAccountRepository.findByEmail("unknown@coffeehut.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.login("unknown@coffeehut.com", "anypassword");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
        assertThat(body.get("error")).isEqualTo("Invalid email or password");
    }

    // ─── NFR7: Reliability — correct HTTP codes and descriptive messages ────

    /**
     * Verifies that the error response body contains a descriptive
     * {@code "error"} message when login fails.
     * <p>
     * The frontend relies on this field to display a user-facing error.
     * A missing or empty message would leave the user without feedback (NFR7).
     * </p>
     *
     * @throws AssertionError if the error field is absent or blank
     * Covers NFR7
     */
    @Test
    void login_withInvalidCredentials_returnsDescriptiveErrorMessage() {
        when(staffAccountRepository.findByEmail("bad@coffeehut.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.login("bad@coffeehut.com", "pass");

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error").toString()).isNotBlank();
    }

    /**
     * Verifies that {@code login()} returns HTTP 401 when both email
     * and password are empty strings.
     * <p>
     * Boundary case: empty credentials must be rejected rather than
     * causing a server error (NFR7 — backend must not return 500).
     * </p>
     *
     * @throws AssertionError if the response status is not 401 or an exception is thrown
     * Covers NFR7
     */
    @Test
    void login_withEmptyCredentials_returnsUnauthorizedNotServerError() {
        when(staffAccountRepository.findByEmail(""))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.login("", "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Verifies that {@code login()} returns HTTP 401 and not HTTP 500
     * when the email exists but the password is {@code null}.
     * <p>
     * Boundary case: a {@code null} password must be handled gracefully
     * without throwing a {@link NullPointerException} (NFR7).
     * </p>
     *
     * @throws AssertionError if a 500 response or exception is produced
     * Covers NFR7
     */
    @Test
    void login_withNullPassword_returnsUnauthorizedGracefully() {
        when(staffAccountRepository.findByEmail("admin@coffeehut.com"))
                .thenReturn(Optional.of(staffAccount));

        ResponseEntity<?> response = authService.login("admin@coffeehut.com", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
