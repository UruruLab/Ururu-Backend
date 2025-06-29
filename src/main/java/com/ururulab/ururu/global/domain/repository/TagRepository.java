package com.ururulab.ururu.global.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ururulab.ururu.global.domain.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
