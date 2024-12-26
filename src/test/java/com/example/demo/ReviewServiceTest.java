package com.example.demo;

import com.example.demo.exception.ReviewNotFoundException;
import com.example.demo.model.Review;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    private Review review;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        review = new Review();
        review.setId(1L);
        review.setTitle("Test Review");
        review.setContent("This is a test review.");
    }

    @Test
    public void testFindById_Success() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // Act
        Review foundReview = reviewService.findById(1L);

        // Assert
        assertNotNull(foundReview);
        assertEquals("Test Review", foundReview.getTitle());
    }

    @Test
    public void testFindById_NotFound() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReviewNotFoundException.class, () -> reviewService.findById(1L));
    }

    @Test
    public void testSaveReview() {
        // Arrange
        when(reviewRepository.save(review)).thenReturn(review);

        // Act
        Review savedReview = reviewService.save(review);

        // Assert
        assertNotNull(savedReview);
        assertEquals("Test Review", savedReview.getTitle());
        verify(reviewRepository, times(1)).save(review);
    }
}
