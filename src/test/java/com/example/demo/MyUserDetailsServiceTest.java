package com.example.demo;

import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MyUserDetailsServiceTest {

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("password");
    }

    @Test
    public void testLoadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = myUserDetailsService.loadUserByUsername("testUser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testUser", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
    }

    @Test
    public void testLoadUserByUsername_NotFound() {
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> myUserDetailsService.loadUserByUsername("nonExistentUser"));
    }
}
