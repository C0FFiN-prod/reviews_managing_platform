package com.example.demo;

import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setPassword("password");
    }

    @Test
    public void testSaveUser() {
        // Arrange
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User savedUser = userService.save(user);

        // Assert
        assertNotNull(savedUser);
        assertEquals("testUser", savedUser.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testFindById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        User foundUser = userService.findById(1L);

        // Assert
        assertNotNull(foundUser);
        assertEquals("testUser", foundUser.getUsername());
    }

    @Test
    public void testFindById_NotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.findById(1L));
    }

    @Test
    public void testDeleteUser() {
        // Arrange
        doNothing().when(userRepository).delete(user);

        // Act
        userService.delete(user);

        // Assert
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        doNothing().when(userRepository).deleteById(1L);

        // Act
        userService.deleteById(1L);

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }
}
