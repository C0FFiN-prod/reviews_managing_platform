package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

public record MyUserDetails(User user) implements UserDetails {

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().toString()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    public LocalDateTime getRegistrationDate() {
        return user.getRegistrationDate();
    }

    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Long getId() {
        return user.getId();
    }

    public Role getRole() {
        return user.getRole();
    }

}
