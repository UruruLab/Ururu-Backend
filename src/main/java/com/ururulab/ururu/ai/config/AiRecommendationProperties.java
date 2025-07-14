package com.ururulab.ururu.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;

/**
 * AI 추천 설정 프로퍼티.
 * application.yml의 app.ai.recommendation 섹션과 매핑됩니다.
 * 
 * 모든 값은 yml 파일에서 설정되어야 하며, 기본값은 제공하지 않습니다.
 */
@Component
@ConfigurationProperties(prefix = "app.ai.recommendation")
@Getter
@Setter
@Validated
public class AiRecommendationProperties {

    /**
     * 기본 추천 개수
     * yml에서 app.ai.recommendation.default-top-k로 설정
     */
    @NotNull
    private Integer defaultTopK;

    /**
     * 기본 최소 유사도 임계값
     * yml에서 app.ai.recommendation.default-min-similarity로 설정
     */
    @NotNull
    private Double defaultMinSimilarity;

    /**
     * 기본 가격 필터 사용 여부
     * yml에서 app.ai.recommendation.default-use-price-filter로 설정
     */
    @NotNull
    private Boolean defaultUsePriceFilter;
}
