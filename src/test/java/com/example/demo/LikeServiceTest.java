package com.example.demo;

import com.example.demo.model.Like;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.LikeRepository;
import com.example.demo.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LikeServiceTest {

    @InjectMocks
    private LikeService likeService;

    @Mock
    private LikeRepository likeRepository;

    private User user;
    private Review review;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        review = new Review();
        review.setId(1L);
    }

    @Test
    public void testToggleLike_AddLike() {
        // Arrange
        when(likeRepository.findByUserAndReview(user, review)).thenReturn(Optional.empty());

        // Act
        boolean result = likeService.toggleLike(user, review);

        // Assert
        assertTrue(result);
        verify(likeRepository, times(1)).save(any(Like.class));
    }

    @Test
    public void testToggleLike_RemoveLike() {
        // Arrange
        Like existingLike = new Like();
        existingLike.setUser(user);
        existingLike.setReview(review);
        when(likeRepository.findByUserAndReview(user, review)).thenReturn(Optional.of(existingLike));

        // Act
        boolean result = likeService.toggleLike(user, review);

        // Assert
        assertFalse(result);
        verify(likeRepository, times(1)).delete(existingLike);
    }

    @Test
    public void testIsLiked_ReturnsTrue() {
        // Arrange
        Like existingLike = new Like();
        existingLike.setUser(user);
        existingLike.setReview(review);
        when(likeRepository.findByUserAndReview(user, review)).thenReturn(Optional.of(existingLike));

        // Act
        boolean result = likeService.isLiked(user, review);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsLiked_ReturnsFalse() {
        // Arrange
        when(likeRepository.findByUserAndReview(user, review)).thenReturn(Optional.empty());

        // Act
        boolean result = likeService.isLiked(user, review);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testCountLikes() {
        // Arrange
        when(likeRepository.countByReview(review)).thenReturn(5L);

    }
}