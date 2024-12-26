package com.example.demo.repository;

import com.example.demo.model.Like;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndReview(User user, Review review);

    long countByReview(Review review);
}