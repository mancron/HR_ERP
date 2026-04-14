package com.hrms.sys.util;

import com.hrms.sys.dto.RagContextDTO;

import java.util.List;

/**
 * PromptAssembler
 * ================
 * RAG 서버 결과(카테고리 + 예제)를 받아 최종 LLM 프롬프트를 조립.
 *
 * 역할 분담:
 *   DynamicPromptBuilder  — DDL / RULES / 헤더 문자열 보유 (재활용)
 *   PromptAssembler       — RAG 결과를 기반으로 DynamicPromptBuilder 조각을 엮음
 *
 * 폴백 시:
 *   ragAvailable=false → DynamicPromptBuilder.build() 직접 호출 (기존 로직 그대로)
 */
public class PromptAssembler {

    private PromptAssembler() {}

    /**
     * RAG 결과로 프롬프트 조립 (정상 경로).
     *
     * @param ragContext   RAG 서버 응답 DTO
     * @param userQuestion 사용자 원본 질문
     * @return 완성된 프롬프트 문자열
     */
    public static String assemble(RagContextDTO ragContext, String userQuestion) {

        List<String> categories = ragContext.getMatchedCategories();

        // RAG가 카테고리를 못 찾은 경우 → EMPLOYEE 기본값
        if (categories == null || categories.isEmpty()) {
            System.out.println("[PromptAssembler] 카테고리 없음 → EMPLOYEE 기본값 사용");
            return DynamicPromptBuilder.buildByCategories(
                    List.of("EMPLOYEE"), null, userQuestion
            );
        }

        System.out.println("[PromptAssembler] 카테고리=" + categories
                + ", 예제=" + ragContext.getFewShotExamples().size() + "개");

        return DynamicPromptBuilder.buildByCategories(
                categories,
                ragContext.getFewShotExamples(),
                userQuestion
        );
    }

    /**
     * 폴백 경로 — RAG 서버 불가 시 기존 키워드 매칭 방식으로 프롬프트 생성.
     *
     * @param userQuestion 사용자 원본 질문
     * @return DynamicPromptBuilder v5 결과 (키워드 매칭)
     */
    public static String assembleFallback(String userQuestion) {
        System.out.println("[PromptAssembler] 폴백 모드 — DynamicPromptBuilder.build() 사용");
        return DynamicPromptBuilder.build(userQuestion);
    }
}
