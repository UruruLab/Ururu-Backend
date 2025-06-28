package com.ururulab.ururu.global.domain.entity;

import com.ururulab.ururu.product.domain.entity.ProductTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tag_category")
public class TagCategory extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(nullable = false)
	private Integer displayOrder;

	@Column(nullable = false)
	private Boolean isActive = true;

	// TagCategory - ProductTag 연관관계 설정 TagCategory 삭제 . ProductTag도 삭제
	@OneToMany(mappedBy = "tagCategory", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductTag> productTags = new ArrayList<>();
}
