package com.sol.office_app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class AppUser {
    @Id
    private Long id;

    private String username;

    private String password;

    @ManyToOne
    private Branch sol;

    private String role;
}
