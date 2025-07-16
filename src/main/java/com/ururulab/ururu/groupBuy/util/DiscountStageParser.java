package com.ururulab.ururu.groupBuy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Slf4j
public class DiscountStageParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 공동 구매 조회 시
     * 문자열(JSON) -> List<DiscountStageDto>
     * 여러 가지 JSON 구조를 모두 처리:
     * 1. {"stages": [...]} - 기존 구조
     * 2. [...] - 배열 직접 구조 (새로운 구조)
     * 3. count/rate 필드와 minQuantity/discountRate 필드 모두 지원
     */
    public static List<DiscountStageDto> parseDiscountStages(String discountStagesJson) {
        if (discountStagesJson == null || discountStagesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(discountStagesJson);
            if (root.isObject() && root.has("stages")) {
                JsonNode stagesNode = root.get("stages");
                if (stagesNode.isArray()) {
                    return parseStagesArray(stagesNode);
                } else {
                    log.warn("stages 필드가 배열이 아닙니다: {}", discountStagesJson);
                    return Collections.emptyList();
                }
            }
            else if (root.isArray()) {
                return parseStagesArray(root);
            }
            else {
                log.warn("지원되지 않는 JSON 구조: {}", discountStagesJson);
                return Collections.emptyList();
            }

        } catch (JsonProcessingException e) {
            log.error("할인 단계 JSON 파싱 실패: {}", discountStagesJson, e);
            throw new BusinessException(DISCOUNT_STAGES_PARSING_FAILED);
        }
    }

    /**
     * 배열 노드를 파싱하여 DiscountStageDto 리스트로 변환
     * count/rate 필드와 minQuantity/discountRate 필드 모두 지원
     */
    private static List<DiscountStageDto> parseStagesArray(JsonNode arrayNode) {
        List<DiscountStageDto> stages = new ArrayList<>();

        for (JsonNode node : arrayNode) {
            try {
                DiscountStageDto stage = objectMapper.convertValue(node, DiscountStageDto.class);
                stages.add(stage);

                log.debug("할인 단계 파싱 성공: {}", stage);
            } catch (Exception e) {
                log.warn("할인 단계 파싱 실패, 건너뜀: {}", node, e);
            }
        }

        return stages;
    }

    /**
     * 공동 구매 등록 시
     * List<DiscountStageDto> -> JSON 문자열
     */
    public static String toJsonString(List<DiscountStageDto> discountStages) {
        try {
            return objectMapper.writeValueAsString(discountStages);
        } catch (JsonProcessingException e) {
            log.error("할인 단계 JSON 직렬화 실패", e);
            throw new BusinessException(DISCOUNT_STAGES_PARSING_FAILED);
        }
    }

    /**
     * JSON에서 최대 할인율만 추출
     * GroupBuy 엔티티의 maxDiscountRate 필드 계산에 사용
     */
    public static Integer extractMaxDiscountRate(String discountStagesJson) {
        if (discountStagesJson == null || discountStagesJson.trim().isEmpty()) {
            return 0;
        }

        try {
            List<DiscountStageDto> stages = parseDiscountStages(discountStagesJson);
            if (stages.isEmpty()) {
                return 0;
            }

            // 할인율이 오름차순으로 정렬되어 있다고 가정하고 마지막 요소 반환
            return stages.get(stages.size() - 1).discountRate();

        } catch (Exception e) {
            log.warn("Failed to extract max discount rate from JSON: {}", discountStagesJson, e);
            return 0; // 예외 발생 시 안전한 기본값
        }
    }
}
