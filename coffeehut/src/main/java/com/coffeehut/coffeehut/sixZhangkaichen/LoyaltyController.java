package com.coffeehut.coffeehut.sixZhangkaichen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for the customer loyalty scheme.
 * <p>
 * Exposes endpoints under {@code /api/loyalty} for member registration,
 * login, order counting, and stamp management. All endpoints return a
 * consistent member response map on success, or a {@code 400 Bad Request}
 * with an error message on failure.
 * </p>
 */
@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(origins = "*")
public class LoyaltyController {

    /** Service layer that contains all loyalty business logic. */
    @Autowired
    private LoyaltyService loyaltyService;

    /**
     * Registers a new loyalty member.
     * <p>
     * Expects a JSON body containing {@code name}, {@code email}, and
     * {@code password}. Delegates validation and persistence to
     * {@link LoyaltyService#register(String, String, String)}.
     * Returns {@code 400} if the email is already registered or any
     * required field is blank.
     * </p>
     *
     * @param body a map containing {@code name}, {@code email},
     *             and {@code password} extracted from the request body
     * @return HTTP 200 with a member response map on success,
     *         or HTTP 400 with an error message on failure
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String name     = body.get("name");
            String email    = body.get("email");
            String password = body.get("password");

            Member member = loyaltyService.register(name, email, password);

            return ResponseEntity.ok(buildMemberResponse(member, "Register success"));
        } catch (RuntimeException e) {
            // Validation or duplicate-email errors are surfaced as 400 Bad Request
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Authenticates an existing loyalty member.
     * <p>
     * Expects a JSON body containing {@code email} and {@code password}.
     * Delegates credential checking to
     * {@link LoyaltyService#login(String, String)}.
     * Returns {@code 400} if the email is not found or the password
     * does not match.
     * </p>
     *
     * @param body a map containing {@code email} and {@code password}
     *             extracted from the request body
     * @return HTTP 200 with a member response map on success,
     *         or HTTP 400 with an error message on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String password = body.get("password");

            Member member = loyaltyService.login(email, password);

            return ResponseEntity.ok(buildMemberResponse(member, "Login success"));
        } catch (RuntimeException e) {
            // Wrong credentials or unknown email surfaced as 400 Bad Request
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retrieves the loyalty profile for a single member.
     * <p>
     * Used by the frontend to reload stamp counts and free-cup balance
     * after the member logs in or completes an order.
     * Returns {@code 400} if no member exists for the given {@code id}.
     * </p>
     *
     * @param id the unique identifier of the loyalty member
     * @return HTTP 200 with a member response map on success,
     *         or HTTP 400 with an error message if the member is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMember(@PathVariable Long id) {
        try {
            Member member = loyaltyService.getMemberById(id);
            return ResponseEntity.ok(buildMemberResponse(member, "Load success"));
        } catch (RuntimeException e) {
            // Member not found is surfaced as 400 Bad Request
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Increments the total order count for a loyalty member by one.
     * <p>
     * Called after a customer successfully places an order so that
     * stamp progress is kept up to date. Every 10 orders should
     * trigger a free cup — that logic lives in {@link LoyaltyService}.
     * Returns {@code 400} if no member exists for the given {@code id}.
     * </p>
     *
     * @param id the unique identifier of the loyalty member
     * @return HTTP 200 with the updated member response map on success,
     *         or HTTP 400 with an error message if the member is not found
     */
    @PostMapping("/{id}/add-order")
    public ResponseEntity<?> addOrder(@PathVariable Long id) {
        try {
            Member member = loyaltyService.addOneOrder(id);
            return ResponseEntity.ok(buildMemberResponse(member, "Order count updated"));
        } catch (RuntimeException e) {
            // Member not found is surfaced as 400 Bad Request
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Updates the stamp counts ({@code totalOrders} and/or {@code freeCups})
     * for a loyalty member.
     * <p>
     * Accepts an optional {@code totalOrders} and/or {@code freeCups} field
     * in the request body. Fields that are absent are left unchanged.
     * Negative values are clamped to zero by the service layer.
     * Returns {@code 400} if no member exists for the given {@code id}.
     * </p>
     *
     * @param id   the unique identifier of the loyalty member
     * @param body a map optionally containing {@code totalOrders} and/or
     *             {@code freeCups} as numeric values
     * @return HTTP 200 with the updated member response map on success,
     *         or HTTP 400 with an error message if the member is not found
     */
    @PostMapping("/{id}/update-stamps")
    public ResponseEntity<?> updateStamps(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            // Extract optional fields — absent keys are treated as null (no change)
            Integer totalOrders = body.containsKey("totalOrders")
                    ? ((Number) body.get("totalOrders")).intValue() : null;
            Integer freeCups = body.containsKey("freeCups")
                    ? ((Number) body.get("freeCups")).intValue() : null;

            Member member = loyaltyService.updateStamps(id, totalOrders, freeCups);
            return ResponseEntity.ok(buildMemberResponse(member, "Stamps updated"));
        } catch (RuntimeException e) {
            // Member not found or invalid input surfaced as 400 Bad Request
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Builds a consistent response map from a {@link Member} entity.
     * <p>
     * Used by all endpoints to produce a uniform JSON structure,
     * avoiding direct serialisation of the JPA entity and ensuring
     * only the fields required by the frontend are exposed.
     * </p>
     *
     * @param member  the loyalty member whose data should be included
     * @param message a human-readable status message to include in the response
     * @return a map containing {@code message}, {@code memberId}, {@code name},
     *         {@code email}, {@code totalOrders}, and {@code freeCups}
     */
    private Map<String, Object> buildMemberResponse(Member member, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message",     message);
        result.put("memberId",    member.getId());
        result.put("name",        member.getName());
        result.put("email",       member.getEmail());
        result.put("totalOrders", member.getTotalOrders());
        result.put("freeCups",    member.getFreeCups());
        return result;
    }
}