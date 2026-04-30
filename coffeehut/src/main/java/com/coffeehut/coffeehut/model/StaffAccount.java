// StaffAccount.java — JPA entity for the staff_accounts table -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing a staff member account.
 * <p>
 * Staff accounts are stored in the {@code staff_accounts} table and are
 * completely separate from the customer loyalty {@link Member} table.
 * Accounts are seeded at application startup via {@code CoffeehutApplication}
 * and are not created through the public API.
 * </p>
 */
@Entity
@Table(name = "staff_accounts")
@Data
public class StaffAccount {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full name of the staff member. */
    private String name;

    /** Unique email address used for staff login. */
    @Column(unique = true)
    private String email;

    /** Plain-text password (to be hashed in future iterations). */
    private String password;
}