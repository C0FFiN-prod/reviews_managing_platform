package com.example.demo.service;

import com.example.demo.model.Bookmark;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

    public boolean isBookmarked(User user, Review review) {
        return bookmarkRepository.findByUserAndReview(user, review).isPresent();
    }

    public boolean toggleBookmark(User user, Review review) {
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserAndReview(user, review);
        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
            return false;
        } else {
            Bookmark newBookmark = new Bookmark();
            newBookmark.setUser(user);
            newBookmark.setReview(review);
            bookmarkRepository.save(newBookmark);
            return true;
        }
    }

    public List<Bookmark> findAllByUser(User user) {
        return bookmarkRepository.findAllByUser(user);
    }
}
