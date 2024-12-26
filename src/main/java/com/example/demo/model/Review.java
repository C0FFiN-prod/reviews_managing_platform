package com.example.demo.model;

import com.example.demo.enums.Status;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.Utils.stringToList;

@Data
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shortReview;

    private String model;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Cascade(CascadeType.SAVE_UPDATE)
    @ColumnDefault("(-(1))")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Cascade(CascadeType.SAVE_UPDATE)
    @ColumnDefault("(-(1))")
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Cascade(CascadeType.SAVE_UPDATE)
    @ColumnDefault("(-(1))")
    @JoinColumn(name = "category_id")
    private Category category;

    private int rating;

    @Lob
    @Column(length = 256)
    private String content;

    @Lob
    @Column(length = 256)
    private String pros;

    @Lob
    @Column(length = 256)
    private String cons;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    ;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private Long likes = 0L;

    public List<String> getPros() {
        return stringToList(pros);
    }

    public List<String> getCons() {
        return stringToList(cons);
    }
}
