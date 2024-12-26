package com.example.demo.repository;

import com.example.demo.dto.CategoryBrandDTO;
import com.example.demo.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByName(String name);

    @NonNull
    Optional<Brand> findById(@NonNull Long id);

    void deleteById(@NonNull Long id);

    void deleteByName(@NonNull String name);

    @Query("""
            SELECT b FROM Brand b
            INNER JOIN CategoryBrand cb ON b.id = cb.brand.id
            WHERE cb.category.id = :categoryId
            """)
    List<Brand> findBrandsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
             SELECT new com.example.demo.dto.CategoryBrandDTO(cb.category.id, b)
             FROM Brand b
             INNER JOIN CategoryBrand cb ON b.id = cb.brand.id
             WHERE cb.category.id IN :categoryIds
            \s""")
    List<CategoryBrandDTO> findCategoryBrandPairsByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    Page<Brand> findPageByNameContainingIgnoreCase(String name, Pageable pageable);
}
