package com.ururulab.ururu.image.domain;

import lombok.Getter;

@Getter
public enum ImageCategory {

	PRODUCTS("products"),
	REVIEWS("reviews"),
	GROUPBUY_THUMBNAIL("groupbuy/thumbnail"),
	GROUPBUY_DETAIL("groupbuy/detail");

	private final String path;

	ImageCategory(String path) {
		this.path = path;
	}

}
