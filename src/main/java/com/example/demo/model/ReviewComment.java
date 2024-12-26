package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@ToString
@Table(name = "review_comments")
public class ReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @ColumnDefault("(-(1))")
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Lob
    @Column(name = "comment_text", nullable = false, length = 256)
    private String commentText;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    ;

    @ColumnDefault("0")
    @Column(name = "is_anonymous")
    private Boolean isAnonymous;

}