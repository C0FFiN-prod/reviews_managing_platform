package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 4, max = 32, message = "Username must be between 4 and 32 characters")
    @Column(unique = true, nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 80)
    private String password;

    @Column(length = 15)
    @Pattern(regexp = "(\\+\\d{5,15})?", message = "Phone number must be in the format +1234567890")
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 60)
    private String email;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "registration_date")
    private LocalDateTime registrationDate = LocalDateTime.now();
    ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(length = 60)
    private String visibleName = this.username;

    private Long reviewCount = 0L;

    private String avatarPath;

    public String getRoleString() {
        return this.role.toString();
    }
}
