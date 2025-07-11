package com.ururulab.ururu.global.domain.repository;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {
    List<TagCategory> findAllByIsActiveTrueOrderByDisplayOrder();
}
