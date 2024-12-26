package com.example.demo;

import com.example.demo.exception.ReviewNotFoundException;
import com.example.demo.model.Review;
import com.example.demo.model.ReviewComment;
import com.example.demo.model.User;
import com.example.demo.repository.ReviewCommentRepository;
import com.example.demo.service.ReviewCommentService;
import com.example.demo.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReviewCommentServiceTest {

    @InjectMocks
    private ReviewCommentService reviewCommentService;

    @Mock
    private ReviewCommentRepository reviewCommentRepository;

    @Mock
    private ReviewService reviewService;

    private Review review;
    private ReviewComment reviewComment;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        review = new Review();
        review.setId(1L);

        reviewComment = new ReviewComment();
        reviewComment.setId(1L);
        reviewComment.setReview(review);
        reviewComment.setCommentText("This is a comment");
    }

    @Test
    public void testFindByReviewId_Success() {
        // Arrange
        when(reviewCommentRepository.findByReviewId(1L)).thenReturn(List.of(reviewComment));

        // Act
        List<ReviewComment> comments = reviewCommentService.findByReviewId(1L);

        // Assert
        assertNotNull(comments);
        assertEquals(1, comments.size());
        assertEquals("This is a comment", comments.get(0).getCommentText());
        verify(reviewCommentRepository, times(1)).findByReviewId(1L);
    }

}