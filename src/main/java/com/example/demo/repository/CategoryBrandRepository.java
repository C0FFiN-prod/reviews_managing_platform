package com.example.demo.repository;

import com.example.demo.model.Category;
import com.example.demo.model.CategoryBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface CategoryBrandRepository extends JpaRepository<CategoryBrand, Long> {
    @NonNull
    Optional<CategoryBrand> findById(@NonNull Long id);

    List<CategoryBrand> findAllByBrandId(@NonNull Long brandId);

    List<CategoryBrand> findAllByCategoryId(@NonNull Long categoryId);

    Optional<CategoryBrand> findAllByCategoryIdAndBrandId(Long categoryId, Long brandId);

}
