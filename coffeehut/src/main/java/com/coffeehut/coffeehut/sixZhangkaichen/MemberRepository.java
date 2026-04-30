package com.coffeehut.coffeehut.sixZhangkaichen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository for loyalty {@link Member} entities.
 * <p>
 * Provides standard CRUD operations inherited from {@link JpaRepository},
 * plus custom queries for email-based lookup and existence checks.
 * The bean is explicitly named {@code loyaltyMemberRepository} to avoid
 * a Spring context conflict with any other {@code MemberRepository}
 * definition that may exist elsewhere in the application.
 * </p>
 * <p>
 * Used exclusively by {@link LoyaltyService} — staff authentication uses
 * a separate {@code StaffAccountRepository} and never touches this table.
 * </p>
 */
@Repository("loyaltyMemberRepository")
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * Finds a loyalty member by their email address.
     * <p>
     * Email values are stored in lower-case by {@link LoyaltyService}, so
     * callers must normalise the supplied address to lower-case before
     * invoking this method to ensure a consistent match.
     * </p>
     *
     * @param email the email address to search for; should be lower-case
     * @return an {@link Optional} containing the matching {@link Member},
     *         or an empty {@link Optional} if no member is registered
     *         with that address
     */
    Optional<Member> findByEmail(String email);

    /**
     * Checks whether a loyalty member with the given email address already exists.
     * <p>
     * Used during registration to enforce the uniqueness constraint at the
     * application level before attempting to persist a new {@link Member},
     * allowing a descriptive error message to be returned rather than
     * propagating a database constraint violation.
     * </p>
     *
     * @param email the email address to check; should be lower-case
     * @return {@code true} if a member with that email address exists,
     *         {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
