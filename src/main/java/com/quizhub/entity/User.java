package com.quizhub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role = "STUDENT";

    @Column(nullable = false)
    private Long xp = 0L;

    protected User() {}

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.xp = 0L;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getXp() { return xp == null ? 0L : xp; }
    public void setXp(Long xp) { this.xp = xp; }
    public void addXp(long delta) { this.xp = getXp() + Math.max(0, delta); }

    public int getLevel() {
        long points = getXp();
        if (points >= 1500) return 6;
        if (points >= 1000) return 5;
        if (points >= 600) return 4;
        if (points >= 300) return 3;
        if (points >= 100) return 2;
        return 1;
    }

    public String getRank() {
        long points = getXp();
        if (points >= 1500) return "Grandmaster";
        if (points >= 1000) return "Master";
        if (points >= 600) return "Diamond";
        if (points >= 300) return "Platinum";
        if (points >= 100) return "Gold";
        return "Bronze";
    }
}
