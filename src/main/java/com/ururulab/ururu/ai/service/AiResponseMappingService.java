package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 서비스 응답을 Spring Boot 응답 형식으로 변환하는 서비스.
 * 
 * 핵심 책임: 상품 ID -> 공동구매 ID 매핑 및 응답 변환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiResponseMappingService {

    private final GroupBuyRepository groupBuyRepository;

    /**
     * AI 서비스 응답을 RecommendedGroupBuy 리스트로 변환.
     * 상품 ID를 실제 공동구매 ID로 매핑하여 정확한 정보 제공
     *
     * @param aiResponse AI 서비스 원본 응답
     * @return 변환된 추천 상품 리스트
     * @throws BusinessException 응답 변환 실패 시
     */
    @SuppressWarnings("unchecked")
    public List<RecommendedGroupBuy> mapToRecommendedGroupBuys(final Map<String, Object> aiResponse) {
        try {
            log.info("[DEBUG] AI 응답 변환 시작 - 전체 응답: {}", aiResponse);

            final List<Map<String, Object>> recommendations = 
                (List<Map<String, Object>>) aiResponse.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                log.warn("AI 응답에서 recommendations 필드가 없거나 비어있음");
                return List.of();
            }

            log.info("[DEBUG] AI recommendations 개수: {}", recommendations.size());

            // 1. AI 응답에서 상품 ID들 추출
            final List<Long> productIds = extractProductIds(recommendations);
            log.info("[DEBUG] AI 추천 상품 ID 목록: {}", productIds);

            // 2. 상품 ID로 활성 공동구매 조회
            final Map<Long, GroupBuy> productToGroupBuyMap = getProductToGroupBuyMapping(productIds);
            log.info("[DEBUG] 매핑된 공동구매 개수: {}/{}", productToGroupBuyMap.size(), productIds.size());

            // 3. 공동구매가 있는 상품만 변환
            final List<RecommendedGroupBuy> result = recommendations.stream()
                    .map(rec -> mapSingleRecommendation(rec, productToGroupBuyMap))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            log.info("[DEBUG] AI 응답 변환 완료 - 변환된 추천 수: {}", result.size());
            return result;
                    
        } catch (final Exception e) {
            log.error("[ERROR] AI 응답 변환 실패", e);
            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE_FORMAT);
        }
    }

    /**
     * AI 추천 결과에서 상품 ID들 추출
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractProductIds(final List<Map<String, Object>> recommendations) {
        return recommendations.stream()
                .map(rec -> {
                    final Map<String, Object> productInfo = (Map<String, Object>) rec.get("product");
                    return productInfo != null ? extractProductId(productInfo) : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 상품 ID -> 공동구매 매핑 생성
     * 한 번의 쿼리로 모든 매핑 정보 조회 (N+1 문제 방지)
     */
    private Map<Long, GroupBuy> getProductToGroupBuyMapping(final List<Long> productIds) {
        try {
            log.info("[DEBUG] 공동구매 매핑 시작 - 상품 ID들: {}", productIds);
            
            if (productIds.isEmpty()) {
                log.info("[DEBUG] 상품 ID가 비어있음");
                return Map.of();
            }

            final List<GroupBuy> activeGroupBuys = groupBuyRepository.findActiveGroupBuysByProductIds(productIds);
            log.info("[DEBUG] 조회된 활성 공동구매 개수: {}", activeGroupBuys.size());
            
            // 한 상품에 여러 공동구매가 있을 수 있으므로 최신 것 선택
            final Map<Long, GroupBuy> result = activeGroupBuys.stream()
                    .collect(Collectors.toMap(
                            gb -> gb.getProduct().getId(),
                            gb -> gb,
                            (existing, replacement) -> 
                                replacement.getCreatedAt().isAfter(existing.getCreatedAt()) ? replacement : existing
                    ));
            
            log.info(" [DEBUG] 매핑 완료 - 결과: {}", result.keySet());
            return result;
            
        } catch (final Exception e) {
            log.error("🚨 [ERROR] 공동구매 매핑 실패", e);
            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE_FORMAT);
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendedGroupBuy mapSingleRecommendation(
            final Map<String, Object> aiRecommendation, 
            final Map<Long, GroupBuy> productToGroupBuyMap) {
        try {
            log.info(" [DEBUG] 개별 추천 매핑 시작 - AI 데이터: {}", aiRecommendation);
            
            final Map<String, Object> productInfo = 
                (Map<String, Object>) aiRecommendation.get("product");
            
            if (productInfo == null) {
                log.warn("AI 추천 결과에 product 정보가 없음");
                return null;
            }

            log.info(" [DEBUG] Product 정보: {}", productInfo);

            // 상품 ID 추출
            final Long productId = extractProductId(productInfo);
            if (productId == null) {
                log.warn("AI 추천 결과에 상품 ID가 없음");
                return null;
            }

            log.info(" [DEBUG] 추출된 상품 ID: {}", productId);

            // 해당 상품의 공동구매 찾기
            final GroupBuy groupBuy = productToGroupBuyMap.get(productId);
            if (groupBuy == null) {
                log.debug("상품 ID {}에 해당하는 활성 공동구매가 없음", productId);
                return null;
            }

            log.info(" [DEBUG] 매핑된 공동구매: ID={}, 제목={}", groupBuy.getId(), groupBuy.getTitle());

            // AI 추천 데이터 추출
            final String productName = extractProductName(productInfo);
            final BigDecimal originalPrice = extractPrice(productInfo);
            final Double similarity = extractSimilarity(aiRecommendation);
            final String recommendReason = extractRecommendReason(aiRecommendation);

            log.info(" [DEBUG] 추출된 데이터 - 상품명: {}, 가격: {}, 유사도: {}", 
                     productName, originalPrice, similarity);

            // 공동구매 정보로 응답 생성
            final RecommendedGroupBuy result = RecommendedGroupBuy.of(
                    groupBuy.getId(), // 실제 공동구매 ID
                    groupBuy.getTitle(), // 공동구매 제목
                    productId, // 상품 ID
                    productName, // 상품명
                    originalPrice, // 원가
                    groupBuy.getDisplayFinalPrice() != null ? 
                        BigDecimal.valueOf(groupBuy.getDisplayFinalPrice()) : originalPrice, // 할인가
                    groupBuy.getThumbnailUrl() != null ? groupBuy.getThumbnailUrl() : "", // 썸네일
                    similarity, // 유사도 점수
                    "AI추천", // 카테고리
                    List.of(), // 주요 성분 (추후 확장)
                    recommendReason, // 추천 이유
                    0, // 현재 참여자 수 (추후 계산)
                    10, // 최소 참여자 수 (기본값)
                    groupBuy.getEndsAt().atZone(ZoneId.systemDefault()).toLocalDateTime() // 종료일
            );
            
            log.info(" [DEBUG] 추천 결과 생성 성공: {}", result);
            return result;
                    
        } catch (final Exception e) {
            log.error(" [ERROR] 개별 추천 결과 변환 실패 - AI 데이터: {}", aiRecommendation, e);
            return null;
        }
    }

    private Long extractProductId(final Map<String, Object> productInfo) {
        final Object idObj = productInfo.get("id");
        if (idObj == null) {
            return null;
        }
        
        try {
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                final String idStr = (String) idObj;
                if (idStr.trim().isEmpty()) {
                    return null;
                }
                return Long.parseLong(idStr.trim());
            } else {
                log.warn("예상치 못한 상품 ID 타입: {} (값: {})", idObj.getClass().getSimpleName(), idObj);
                return null;
            }
        } catch (final NumberFormatException e) {
            log.warn("상품 ID 변환 실패: {}", idObj, e);
            return null;
        }
    }

    private String extractProductName(final Map<String, Object> productInfo) {
        return (String) productInfo.getOrDefault("name", "상품명 없음");
    }

    private BigDecimal extractPrice(final Map<String, Object> productInfo) {
        final Object priceObj = productInfo.get("base_price");
        if (priceObj == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            if (priceObj instanceof Number) {
                return BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                final String priceStr = (String) priceObj;
                if (priceStr.trim().isEmpty()) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(priceStr.trim());
            } else {
                log.warn("예상치 못한 가격 타입: {} (값: {})", priceObj.getClass().getSimpleName(), priceObj);
                return BigDecimal.ZERO;
            }
        } catch (final NumberFormatException e) {
            log.warn("가격 변환 실패: {} - 기본값 0 사용", priceObj, e);
            return BigDecimal.ZERO;
        }
    }

    private Double extractSimilarity(final Map<String, Object> aiRecommendation) {
        final Object similarityObj = aiRecommendation.get("similarity_score");
        if (similarityObj == null) {
            return 0.0;
        }
        
        try {
            if (similarityObj instanceof Number) {
                return ((Number) similarityObj).doubleValue();
            } else if (similarityObj instanceof String) {
                final String similarityStr = (String) similarityObj;
                if (similarityStr.trim().isEmpty()) {
                    return 0.0;
                }
                return Double.parseDouble(similarityStr.trim());
            } else {
                log.warn("예상치 못한 유사도 타입: {} (값: {})", similarityObj.getClass().getSimpleName(), similarityObj);
                return 0.0;
            }
        } catch (final NumberFormatException e) {
            log.warn("유사도 변환 실패: {} - 기본값 0.0 사용", similarityObj, e);
            return 0.0;
        }
    }

    private String extractRecommendReason(final Map<String, Object> aiRecommendation) {
        return (String) aiRecommendation.getOrDefault("recommendation_reason", "AI 분석 기반 추천");
    }
}
