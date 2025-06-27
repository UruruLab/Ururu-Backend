package com.ururulab.ururu.global.common.repository;

import com.ururulab.ururu.global.common.entity.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {
}
