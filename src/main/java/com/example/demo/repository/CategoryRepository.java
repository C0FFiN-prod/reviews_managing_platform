package com.example.demo.repository;

import com.example.demo.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    @NonNull
    Optional<Category> findById(@NonNull Long id);

    @Query(value = """
                WITH RECURSIVE category_tree AS (
                    SELECT * FROM categories WHERE id = :parentId
                    UNION ALL
                    SELECT c.* FROM categories c
                    INNER JOIN category_tree ct ON c.parent_id = ct.id
                )
                SELECT * FROM category_tree
            """, nativeQuery = true)
    List<Category> findAllSubcategoriesByParentId(Long parentId);

    @Query(value = """
                WITH RECURSIVE parent_tree AS (
                    SELECT * FROM categories WHERE id = :categoryId
                    UNION ALL
                    SELECT p.* FROM categories p
                    INNER JOIN parent_tree pt ON pt.parent_id = p.id
                )
                SELECT * FROM parent_tree
            """, nativeQuery = true)
    List<Category> findAllParentCategories(Long categoryId);

    void deleteById(@NonNull Long id);

    void deleteByName(@NonNull String name);

    @Query("""
             SELECT cb
             FROM Category cb
             WHERE cb.id NOT IN :categoryIds
            """)
    List<Category> findAllExcept(List<Long> categoryIds);

    Page<Category> findPageByNameContainingIgnoreCase(String name, Pageable pageable);
}
