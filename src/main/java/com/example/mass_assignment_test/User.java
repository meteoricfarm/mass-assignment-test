package com.example.mass_assignment_test;

import jakarta.persistence.Entity; // Spring Boot 3+는 jakarta 사용
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "USERS") // USER는 H2의 예약어일 수 있어 USERS로 변경
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String email;
    private String role; // 민감한 필드

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}