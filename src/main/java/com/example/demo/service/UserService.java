package com.example.demo.service;

import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.LoginAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserDataById(Long id, String email, String phone, String visibleName) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id:" + id));
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setVisibleName(visibleName);
        return user;
    }

    @Transactional
    public User updateUserData(User user, String email, String phone, String visibleName) {
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setVisibleName(visibleName);
        return user;
    }

    public void delete(User user) {
        userRepository.delete(user);
    }

    public void deleteByUsername(String username) {
        userRepository.deleteByUsername(username);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new LoginAlreadyExistsException("Login already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
