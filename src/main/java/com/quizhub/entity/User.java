package com.quizhub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(nullable = false, unique = true, length = 100) private String email;
    @Column(nullable = false) private String password;
    @Column(nullable = false, length = 20) private String role = "STUDENT";

    protected User() {}
    public User(String username, String email, String password, String role) {
        this.username = username; this.email = email; this.password = password; this.role = role;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}
