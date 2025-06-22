package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 최상위 카테고리 조회 (parentId가 null인 것들)
    List<Category> findByParentIdIsNullOrderByOrderIndex();

    // 특정 부모 카테고리의 하위 카테고리들 조회
    List<Category> findByParentIdOrderByOrderIndex(Long parentId);

    // 특정 깊이의 카테고리들 조회
    List<Category> findByDepthOrderByOrderIndex(int depth);

    // 카테고리 이름으로 검색
    List<Category> findByNameContainingOrderByDepthAscOrderIndexAsc(String name);

    // 특정 경로를 포함하는 카테고리들 조회
    List<Category> findByPathContaining(String path);

    // 카테고리 계층 구조 조회 (특정 카테고리의 전체 경로)
    @Query("SELECT c FROM Category c WHERE c.path LIKE CONCAT(:path, '%') ORDER BY c.depth, c.orderIndex")
    List<Category> findCategoryHierarchy(@Param("path") String path);

    // 리프 카테고리들 조회 (하위 카테고리가 없는 최하위 카테고리들)
    @Query("SELECT c FROM Category c WHERE c.id NOT IN (SELECT DISTINCT c2.parentId FROM Category c2 WHERE c2.parentId IS NOT NULL)")
    List<Category> findLeafCategories();
}
