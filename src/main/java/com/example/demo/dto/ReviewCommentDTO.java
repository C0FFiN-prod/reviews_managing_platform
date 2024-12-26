package com.example.demo.dto;

import com.example.demo.model.ReviewComment;
import com.example.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewCommentDTO {
    private Long id;
    private String commentText;
    private LocalDateTime createdAt;
    private Boolean isAnonymous;
    private UserDTO user;

    public ReviewCommentDTO(ReviewComment comment) {
        this.id = comment.getId();
        this.commentText = comment.getCommentText();
        this.createdAt = comment.getCreatedAt();
        this.isAnonymous = comment.getIsAnonymous();
        User user = comment.getUser();
        if (user != null) {
            this.user = new UserDTO(user.getId(),
                    user.getVisibleName(),
                    user.getAvatarPath());
        } else {
            this.user = new UserDTO(-1L, "Deleted User", "");
        }
    }
}
