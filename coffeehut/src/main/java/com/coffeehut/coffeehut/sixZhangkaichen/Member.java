package com.coffeehut.coffeehut.sixZhangkaichen;

import jakarta.persistence.*;
@Entity(name = "LoyaltyMember")
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private Integer totalOrders;
    private Integer freeCups = 0;
    public Member() {
    }
    public Member(String name, String email, String password, Integer totalOrders) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.totalOrders = totalOrders;
        this.freeCups = 0;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public Integer getTotalOrders() {
        return totalOrders;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }
    public Integer getFreeCups() {
        return freeCups == null ? 0 : freeCups;
    }
    public void setFreeCups(Integer freeCups) {
        this.freeCups = freeCups;
    }
}