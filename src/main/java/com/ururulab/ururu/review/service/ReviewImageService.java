package com.ururulab.ururu.review.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewImageService {

	public List<String> storeImages(List<MultipartFile> images) {
		return Collections.emptyList();
	}
}
