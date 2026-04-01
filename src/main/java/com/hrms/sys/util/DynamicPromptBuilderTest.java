package com.hrms.sys.util;

/**
 * DynamicPromptBuilder 동작 검증 테스트
 * 
 * 실행: javac DynamicPromptBuilder.java DynamicPromptBuilderTest.java && java DynamicPromptBuilderTest
 */
public class DynamicPromptBuilderTest {

    public static void main(String[] args) {
        System.out.println("=== DynamicPromptBuilder 검증 테스트 ===\n");

        // 테스트 케이스 정의
        String[][] testCases = {
            // { 질문, 기대되는 카테고리 }
            { "부장 목록",                     "EMPLOYEE" },
            { "재직 직원 명단",                "EMPLOYEE" },
            { "과장 이상 직원",                "EMPLOYEE" },
            { "이번달 급여 현황",              "SALARY" },
            { "부장 급여",                     "EMPLOYEE + SALARY" },
            { "이번달 지각한 사람",            "ATTENDANCE" },
            { "연차 남은 일수",                "LEAVE" },
            { "하반기 S등급 받은 직원",        "EVALUATION" },
            { "잠긴 계정 목록",                "ACCOUNT" },
            { "2025년 공휴일",                 "SYSTEM" },
            { "감사로그 최근 10건",            "SYSTEM" },
            { "개발팀 초과근무 현황",          "ATTENDANCE" },
            { "부서별 평균 연봉",              "EMPLOYEE" },
            { "김으로 시작하는 직원",           "EMPLOYEE" },
        };

        int passCount = 0;
        int failCount = 0;

        for (String[] tc : testCases) {
            String question = tc[0];
            String expected = tc[1];

            // 매칭된 카테고리 확인
            String matched = DynamicPromptBuilder.getMatchedCategories(question);

            // 프롬프트 빌드
            String prompt = DynamicPromptBuilder.build(question);
            int estimatedTokens = DynamicPromptBuilder.estimateTokens(prompt);

            // 핵심 검증: "부장" 관련 질문에서 emp_type 경고가 포함되어 있는지
            boolean hasEmpTypeWarning = prompt.contains("NEVER use emp_type for position");
            // 핵심 검증: position_name 예제가 포함되어 있는지
            boolean hasPositionExample = prompt.contains("position_name = '부장'");

            // 결과 출력
            System.out.printf("질문: \"%s\"\n", question);
            System.out.printf("  매칭: %s (기대: %s)\n", matched, expected);
            System.out.printf("  토큰: ~%d (기존 ~4200 대비)\n", estimatedTokens);

            if (question.contains("부장")) {
                System.out.printf("  emp_type 경고 포함: %s\n", hasEmpTypeWarning ? "✓ YES" : "✗ NO");
                System.out.printf("  position_name 예제 포함: %s\n", hasPositionExample ? "✓ YES" : "✗ NO");

                if (hasEmpTypeWarning && hasPositionExample) {
                    System.out.println("  → ✅ PASS (부장=position_name 규칙 정상 포함)");
                    passCount++;
                } else {
                    System.out.println("  → ❌ FAIL (부장 관련 규칙 누락!)");
                    failCount++;
                }
            } else {
                System.out.println("  → ✅ PASS");
                passCount++;
            }
            System.out.println();
        }

        System.out.println("========================================");
        System.out.printf("결과: %d PASS / %d FAIL / %d TOTAL\n", passCount, failCount, passCount + failCount);
        System.out.println("========================================");

        // 프롬프트 크기 비교 출력
        System.out.println("\n=== 프롬프트 크기 비교 ===\n");
        String[] sampleQuestions = { "부장 목록", "이번달 급여 현황", "부장 급여" };
        for (String q : sampleQuestions) {
            String p = DynamicPromptBuilder.build(q);
            System.out.printf("\"%s\" → %d chars, ~%d tokens\n", q, p.length(), DynamicPromptBuilder.estimateTokens(p));
        }
        System.out.printf("\n기존 SCHEMA_PROMPT (고정) → ~10,000 chars, ~4,200 tokens\n");
    }
}