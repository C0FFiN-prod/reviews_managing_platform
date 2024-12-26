package com.example.demo.dto;


import com.example.demo.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatarPath;
    private int rating;
    private String likes;
    private String title;
    private String createdAt;
    private String shortReview;
    private String reviewTeaser;
    private Long totalImages;
    private List<String> imagePaths;
    private Status status;
}
