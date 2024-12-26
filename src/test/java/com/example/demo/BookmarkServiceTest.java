package com.example.demo;

import com.example.demo.model.Bookmark;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.BookmarkRepository;
import com.example.demo.service.BookmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookmarkServiceTest {

    @InjectMocks
    private BookmarkService bookmarkService;

    @Mock
    private BookmarkRepository bookmarkRepository;

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
    public void testToggleBookmark_AddBookmark() {
        // Arrange
        when(bookmarkRepository.findByUserAndReview(user, review)).thenReturn(Optional.empty());

        // Act
        boolean result = bookmarkService.toggleBookmark(user, review);

        // Assert
        assertTrue(result);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @Test
    public void testToggleBookmark_RemoveBookmark() {
        // Arrange
        Bookmark existingBookmark = new Bookmark();
        existingBookmark.setUser(user);
        existingBookmark.setReview(review);
        when(bookmarkRepository.findByUserAndReview(user, review)).thenReturn(Optional.of(existingBookmark));

        // Act
        boolean result = bookmarkService.toggleBookmark(user, review);

        // Assert
        assertFalse(result);
        verify(bookmarkRepository, times(1)).delete(existingBookmark);
    }

    @Test
    public void testIsBookmarked_ReturnsTrue() {
        // Arrange
        Bookmark existingBookmark = new Bookmark();
        existingBookmark.setUser(user);
        existingBookmark.setReview(review);
        when(bookmarkRepository.findByUserAndReview(user, review)).thenReturn(Optional.of(existingBookmark));

        // Act
        boolean result = bookmarkService.isBookmarked(user, review);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsBookmarked_ReturnsFalse() {
        // Arrange
        when(bookmarkRepository.findByUserAndReview(user, review)).thenReturn(Optional.empty());

        // Act
        boolean result = bookmarkService.isBookmarked(user, review);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testFindAllByUser() {
        // Arrange
        when(bookmarkRepository.findAllByUser(user)).thenReturn(List.of());

        // Act
        List<Bookmark> bookmarks = bookmarkService.findAllByUser(user);

        // Assert
        assertNotNull(bookmarks);
        assertTrue(bookmarks.isEmpty());
        verify(bookmarkRepository, times(1)).findAllByUser(user);
    }
}
