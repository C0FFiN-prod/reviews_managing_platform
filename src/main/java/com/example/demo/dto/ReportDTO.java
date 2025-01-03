package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Long reviewId;
    private String description;
    private LocalDateTime createdAt;
}