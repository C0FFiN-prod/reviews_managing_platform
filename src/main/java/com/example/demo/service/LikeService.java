package com.example.demo.service;

import com.example.demo.model.Like;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    public boolean toggleLike(User user, Review review) {
        Optional<Like> existingLike = likeRepository.findByUserAndReview(user, review);
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return false; // Лайк удален
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setReview(review);
            likeRepository.save(like);
            return true; // Лайк добавлен
        }
    }

    public long countLikes(Review review) {
        return likeRepository.countByReview(review);
    }

    public boolean isLiked(User user, Review review) {
        return likeRepository.findByUserAndReview(user, review).isPresent();
    }
}
