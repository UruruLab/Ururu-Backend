package com.ururulab.ururu.review.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ururulab.ururu.review.domain.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
