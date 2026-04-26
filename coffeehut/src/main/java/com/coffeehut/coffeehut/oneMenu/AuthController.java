// AuthController.java — Authentication endpoints -WeiqiWang

package com.coffeehut.coffeehut.oneMenu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("email"), body.get("password"));
    }

    // Registration is intentionally disabled. -WeiqiWang
    // Staff accounts are managed directly in the database via CoffeehutApplication seed data.
    @PostMapping("/register")
    public ResponseEntity<?> register() {
        return ResponseEntity.status(403).body(Map.of("error", "Registration is not allowed"));
    }
}
