package com.ururulab.ururu.review.service;

import static com.ururulab.ururu.review.domain.policy.ReviewImagePolicy.*;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewImageService {

	public List<String> storeImages(List<MultipartFile> images) {
		if (images != null && images.size() > MAX_IMAGE_COUNT) {
			throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGE_COUNT + "개까지 첨부할 수 있습니다.");
		}
		return Collections.emptyList();
	}
}
