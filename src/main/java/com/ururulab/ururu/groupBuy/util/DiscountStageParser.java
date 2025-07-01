package com.ururulab.ururu.groupBuy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.groupBuy.domain.dto.common.DiscountStageDto;
import com.ururulab.ururu.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

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
     */
    public static List<DiscountStageDto> parseDiscountStages(String discountStagesJson) {
        if (discountStagesJson == null || discountStagesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(discountStagesJson);
            JsonNode stagesNode = root.get("stages");

            if (stagesNode == null || !stagesNode.isArray()) {
                log.warn("stages 필드가 없거나 배열이 아닙니다: {}", discountStagesJson);
                return Collections.emptyList();
            }

            return objectMapper.convertValue(stagesNode, new TypeReference<List<DiscountStageDto>>() {});

        } catch (JsonProcessingException e) {
            log.error("할인 단계 JSON 파싱 실패: {}", discountStagesJson, e);
            throw new BusinessException(DISCOUNT_STAGES_PARSING_FAILED);
        }
    }

    /**
     * 공동 구매 등록 시
     * List<DiscountStageDto> -> JSON 문자열 (stages 키 포함)
     */
    public static String toJsonString(List<DiscountStageDto> discountStages) {
        try {
            Map<String, Object> jsonMap = Map.of("stages", discountStages);
            return objectMapper.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            log.error("할인 단계 JSON 직렬화 실패", e);
            throw new BusinessException(DISCOUNT_STAGES_PARSING_FAILED);
        }
    }
}
