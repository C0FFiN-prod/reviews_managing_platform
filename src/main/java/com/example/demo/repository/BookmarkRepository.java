package com.example.demo.repository;

import com.example.demo.model.Bookmark;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserAndReview(User user, Review review);

    void deleteByUserAndReview(User user, Review review);

    @Query("""
                SELECT b FROM Bookmark b
                WHERE b.user = :user
                AND (b.review.user = :user OR b.review.status = com.example.demo.model.Status.PUBLISHED)
            """)
    List<Bookmark> findAllByUser(@Param("user") User user);
}