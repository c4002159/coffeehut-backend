// StaffAccount.java — JPA entity for the staff_accounts table -WeiqiWang
// Completely separate from the members table used by the loyalty/customer system.
// Staff accounts are managed exclusively via CoffeehutApplication seed data.

package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "staff_accounts")
@Data
public class StaffAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;
}
