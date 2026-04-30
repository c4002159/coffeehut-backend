// AuthController.java — Authentication endpoints -WeiqiWang
package com.coffeehut.coffeehut.oneMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * REST controller handling staff authentication requests.
 * <p>
 * Exposes endpoints under {@code /api/auth} for staff login only.
 * Customer (loyalty member) authentication is handled separately by
 * {@code LoyaltyController}. Staff accounts are seeded at startup and
 * cannot be created through this API.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    /** Service responsible for validating staff credentials. */
    @Autowired
    private AuthService authService;

    /**
     * Authenticates a staff member using email and password.
     * <p>
     * Delegates credential validation to {@link AuthService#login(String, String)}.
     * Returns HTTP 401 if the credentials do not match any staff account.
     * </p>
     *
     * @param body request body containing {@code "email"} and {@code "password"} fields
     * @return HTTP 200 with staff identity on success, or HTTP 401 on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    /**
     * Registration endpoint — permanently disabled.
     * <p>
     * Staff accounts are managed directly in the database via
     * {@code CoffeehutApplication} seed data and must not be created
     * through the public API.
     * </p>
     *
     * @return HTTP 403 with an error message
     */
    // Registration is intentionally disabled. -WeiqiWang
    // Staff accounts are managed directly in the database via CoffeehutApplication seed data.
    @PostMapping("/register")
    public ResponseEntity<?> register() {
        return ResponseEntity.status(403).body(Map.of("error", "Registration is not allowed"));
    }
}