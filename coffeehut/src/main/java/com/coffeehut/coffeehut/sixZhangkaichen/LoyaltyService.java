package com.coffeehut.coffeehut.sixZhangkaichen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Service layer for the customer loyalty stamp scheme.
 * <p>
 * Handles member registration, authentication, order counting, and
 * free-cup stamp management. Every 10 completed orders earns the member
 * one free cup, tracked via {@link MemberOrderLink} to prevent double-counting.
 * All persistence is delegated to {@link MemberRepository} and
 * {@link MemberOrderLinkRepository}.
 * </p>
 */
@Service
public class LoyaltyService {

    /** Repository for {@link Member} CRUD operations. */
    @Autowired
    private MemberRepository memberRepository;

    /**
     * Repository for {@link MemberOrderLink} records.
     * <p>
     * Each link associates one order with one member and tracks whether
     * that order has already been counted toward the member's stamp total.
     * </p>
     */
    @Autowired
    private MemberOrderLinkRepository memberOrderLinkRepository;

    /**
     * Registers a new loyalty member.
     * <p>
     * Validates that {@code name}, {@code email}, and {@code password} are
     * all non-blank, then checks that the email address is not already in use.
     * The email is normalised to lower-case before being stored so that
     * lookups are case-insensitive. {@code totalOrders} is initialised to
     * zero on creation.
     * </p>
     *
     * @param name     the member's display name; must not be blank
     * @param email    the member's email address; must not be blank and
     *                 must not already be registered
     * @param password the member's plain-text password; must not be blank
     * @return the newly created and persisted {@link Member}
     * @throws RuntimeException if any field is blank or the email is
     *                          already registered
     */
    public Member register(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        // Normalise to lower-case so login lookups are case-insensitive
        String cleanEmail = email.trim().toLowerCase();
        if (memberRepository.existsByEmail(cleanEmail)) {
            throw new RuntimeException("This email is already registered");
        }

        Member member = new Member();
        member.setName(name.trim());
        member.setEmail(cleanEmail);
        member.setPassword(password.trim());
        member.setTotalOrders(0);
        return memberRepository.save(member);
    }

    /**
     * Authenticates an existing loyalty member by email and password.
     * <p>
     * The supplied email is normalised to lower-case before the database
     * lookup so that authentication is case-insensitive. Password comparison
     * is performed as a plain-text equality check against the stored value.
     * </p>
     *
     * @param email    the member's registered email address; must not be blank
     * @param password the member's plain-text password; must not be blank
     * @return the authenticated {@link Member}
     * @throws RuntimeException if either field is blank, the email is not
     *                          found, or the password does not match
     */
    public Member login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        // Normalise to lower-case to match the stored value written during registration
        String cleanEmail = email.trim().toLowerCase();
        Member member = memberRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!member.getPassword().equals(password.trim())) {
            throw new RuntimeException("Wrong password");
        }
        return member;
    }

    /**
     * Retrieves a loyalty member by their primary key.
     * <p>
     * Used internally by other service methods and externally by the
     * controller to load a member's current profile.
     * </p>
     *
     * @param id the primary key of the {@link Member} to retrieve
     * @return the matching {@link Member}
     * @throws RuntimeException if no member exists for the given {@code id}
     */
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    /**
     * Increments a member's total order count by one.
     * <p>
     * Guards against a {@code null} {@code totalOrders} value that could
     * exist for legacy records created before the field was introduced,
     * treating {@code null} as zero before incrementing.
     * </p>
     *
     * @param id the primary key of the {@link Member} to update
     * @return the updated {@link Member} after the increment is persisted
     * @throws RuntimeException if no member exists for the given {@code id}
     */
    public Member addOneOrder(Long id) {
        Member member = getMemberById(id);

        // Treat null as 0 to handle legacy members created before this field existed
        if (member.getTotalOrders() == null) {
            member.setTotalOrders(0);
        }
        member.setTotalOrders(member.getTotalOrders() + 1);
        return memberRepository.save(member);
    }

    /**
     * Links an order to a loyalty member so that stamp credit can be
     * awarded when the order is collected.
     * <p>
     * The link is created with {@code counted = false}; it is only marked
     * {@code true} by {@link #handleCollectedOrder(Long)} once the order
     * reaches {@code collected} status. This two-step design prevents stamps
     * from being awarded before the customer actually receives their order,
     * and prevents double-counting if the method is called more than once
     * for the same order.
     * </p>
     * <p>
     * Silently returns without saving if either argument is {@code null},
     * if the member does not exist, or if a link for the order already exists.
     * </p>
     *
     * @param memberId the primary key of the loyalty member placing the order
     * @param orderId  the primary key of the order being placed
     */
    public void saveOrderLink(Long memberId, Long orderId) {
        // Guard against missing IDs — can happen if the customer is not logged in
        if (memberId == null || orderId == null) {
            return;
        }
        // Do not create orphaned links for members that no longer exist
        if (!memberRepository.existsById(memberId)) {
            return;
        }
        // Idempotency guard — do not create a second link for the same order
        Optional<MemberOrderLink> oldLink = memberOrderLinkRepository.findByOrderId(orderId);
        if (oldLink.isPresent()) {
            return;
        }

        MemberOrderLink link = new MemberOrderLink();
        link.setMemberId(memberId);
        link.setOrderId(orderId);
        link.setCounted(false); // will be set to true by handleCollectedOrder
        memberOrderLinkRepository.save(link);
    }

    /**
     * Manually updates a member's {@code totalOrders} and/or {@code freeCups}
     * stamp counts.
     * <p>
     * Either parameter may be {@code null}, in which case the corresponding
     * field is left unchanged. Negative values are clamped to zero to prevent
     * invalid state. This endpoint exists to allow administrative corrections
     * without replaying the entire order history.
     * </p>
     *
     * @param id          the primary key of the {@link Member} to update
     * @param totalOrders the new total order count, or {@code null} to leave
     *                    the existing value unchanged; clamped to {@code 0}
     *                    if negative
     * @param freeCups    the new free-cup balance, or {@code null} to leave
     *                    the existing value unchanged; clamped to {@code 0}
     *                    if negative
     * @return the updated {@link Member} after changes are persisted
     * @throws RuntimeException if no member exists for the given {@code id}
     */
    public Member updateStamps(Long id, Integer totalOrders, Integer freeCups) {
        Member member = getMemberById(id);

        // Only update the fields that were explicitly provided in the request
        if (totalOrders != null) member.setTotalOrders(Math.max(0, totalOrders));
        if (freeCups    != null) member.setFreeCups(Math.max(0, freeCups));
        return memberRepository.save(member);
    }

    /**
     * Awards stamp credit to a loyalty member when their order is collected.
     * <p>
     * Looks up the {@link MemberOrderLink} for the given order. If no link
     * exists the order was placed by a guest (not logged in) and no action
     * is taken. If the link is already marked {@code counted = true} the
     * method returns immediately to prevent double-counting. Otherwise the
     * member's {@code totalOrders} is incremented by one and the link is
     * marked as counted so subsequent calls are idempotent.
     * </p>
     * <p>
     * This method should be called by the order status update flow whenever
     * an order transitions to {@code collected} status.
     * </p>
     *
     * @param orderId the primary key of the order that has just been collected
     */
    public void handleCollectedOrder(Long orderId) {
        Optional<MemberOrderLink> optionalLink =
                memberOrderLinkRepository.findByOrderId(orderId);

        // No link means the order was placed by a guest — nothing to award
        if (optionalLink.isEmpty()) {
            return;
        }

        MemberOrderLink link = optionalLink.get();

        // Idempotency guard — stamps already awarded for this order
        if (Boolean.TRUE.equals(link.getCounted())) {
            return;
        }

        Member member = getMemberById(link.getMemberId());

        // Treat null as 0 to handle legacy members created before this field existed
        if (member.getTotalOrders() == null) {
            member.setTotalOrders(0);
        }
        member.setTotalOrders(member.getTotalOrders() + 1);
        memberRepository.save(member);

        // Mark the link as counted so this block cannot run again for the same order
        link.setCounted(true);
        memberOrderLinkRepository.save(link);
    }
}