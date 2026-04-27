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

@Service
public class AuthService {

    // Queries staff_accounts, never members. -WeiqiWang
    @Autowired
    private StaffAccountRepository staffAccountRepository;

    public ResponseEntity<?> login(String email, String password) {
        Optional<StaffAccount> account = staffAccountRepository.findByEmail(email);
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
