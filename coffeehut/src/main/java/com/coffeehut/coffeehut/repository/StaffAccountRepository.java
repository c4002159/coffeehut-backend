// StaffAccountRepository.java — JPA repository for staff_accounts table -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for managing {@link StaffAccount} entities.
 * <p>
 * This interface provides data access operations for the staff_accounts table
 * using Spring Data JPA. By extending {@link JpaRepository}, it inherits standard
 * CRUD operations such as creating, retrieving, updating, and deleting staff accounts.
 * </p>
 * <p>
 * It is primarily used in authentication and staff management features, allowing
 * lookup of staff records based on unique identifiers such as email.
 * </p>
 */
@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {

    /**
     * Retrieves a staff account by email address.
     * <p>
     * This method performs a query based on the {@code email} field of the
     * {@link StaffAccount} entity. It is typically used during authentication
     * or account verification processes where email is treated as a unique identifier.
     * </p>
     *
     * @param email the email address used to identify the staff account
     * @return an {@link Optional} containing the matching {@link StaffAccount},
     *         or {@link Optional#empty()} if no account is found
     */
    Optional<StaffAccount> findByEmail(String email);
}