package com.ururulab.ururu.global.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ururulab.ururu.global.common.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
