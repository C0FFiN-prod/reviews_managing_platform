package com.example.demo.dto;

import com.example.demo.model.Brand;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryBrandDTO {
    private Long categoryId;
    private Brand brand;
}