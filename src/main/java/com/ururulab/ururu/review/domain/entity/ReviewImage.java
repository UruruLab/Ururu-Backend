package com.ururulab.ururu.review.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "review_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private Integer displayOrder;

	@Column(nullable = false)
	private Boolean isDelete = false;

	static ReviewImage ofAdd(Review review, String imageUrl, Integer displayOrder) {
		ReviewImage reviewImage = new ReviewImage();
		reviewImage.review = review;
		reviewImage.imageUrl = imageUrl;
		reviewImage.displayOrder = displayOrder;
		return reviewImage;
	}
}
