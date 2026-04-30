package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Member} entities.
 * <p>
 * Provides standard CRUD operations inherited from {@link JpaRepository},
 * plus a custom query for email-based member lookup. Used by the customer
 * authentication and loyalty scheme flows to persist and retrieve member
 * account data.
 * </p>
 * <p>
 * Note: this repository operates on the {@code members} table shared with
 * the loyalty scheme. Staff accounts are managed by a separate repository
 * and never interact with this table.
 * </p>
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * Finds a member by their email address.
     * <p>
     * Email values are stored in lower-case by the service layer, so
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
}