// AuthService.java — Staff authentication, queries staff_accounts table only -WeiqiWang
// Does NOT touch the members table (used exclusively by the customer loyalty system).
package com.coffeehut.coffeehut.oneMenu;
import com.coffeehut.coffeehut.model.StaffAccount;
import com.coffeehut.coffeehut.repository.StaffAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for staff authentication.
 * <p>
 * Validates staff credentials against the {@code staff_accounts} table only.
 * This service deliberately does not interact with the {@code members} table,
 * which is reserved exclusively for the customer loyalty system.
 * </p>
 */
@Service
public class AuthService {

    /**
     * Repository for {@link StaffAccount} entities.
     * <p>
     * Queries the {@code staff_accounts} table only — never the {@code members} table.
     * </p>
     */
    // Queries staff_accounts, never members. -WeiqiWang
    @Autowired
    private StaffAccountRepository staffAccountRepository;

    /**
     * Validates staff login credentials and returns account identity on success.
     * <p>
     * Performs a case-sensitive email lookup followed by a plain-text password
     * comparison. Returns HTTP 401 if no matching account is found or if the
     * password does not match.
     * </p>
     *
     * @param email    the email address submitted by the staff member
     * @param password the plain-text password submitted by the staff member
     * @return HTTP 200 with {@code memberId} and {@code name} on success,
     *         or HTTP 401 with an error message if credentials are invalid
     */
    public ResponseEntity<?> login(String email, String password) {
        Optional<StaffAccount> account = staffAccountRepository.findByEmail(email);
        // Reject immediately if no account exists or password does not match
        if (account.isEmpty() || !account.get().getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        StaffAccount a = account.get();
        return ResponseEntity.ok(Map.of(
                "memberId", a.getId(),
                "name",     a.getName()
        ));
    }
}