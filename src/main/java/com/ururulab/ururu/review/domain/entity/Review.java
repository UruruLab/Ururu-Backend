package com.ururulab.ururu.review.domain.entity;

import java.time.LocalDateTime;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Product product;

	@Column(nullable = false)
	private Long productOptionId;

	@Column(nullable = false)
	private Integer rating;

	@Enumerated(EnumType.STRING)
	private SkinType skinType;

	@Enumerated(EnumType.STRING)
	private AgeGroup ageGroup;

	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column(length = 1000)
	private String content;

	@Column(nullable = false)
	private Boolean isDelete = false;

	@Column(nullable = false)
	private Integer likeCount = 0;

}
