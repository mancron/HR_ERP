package com.hrms.sys.util;

import com.hrms.sys.dto.RagContextDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Python RAG 서버(/api/v1/search_context)를 호출하는 HTTP 클라이언트.
 *
 * 설계 원칙:
 *  - 외부 라이브러리(Jackson 등) 없이 순수 Java로 JSON 수동 파싱
 *  - 타임아웃(3초) 초과 또는 서버 다운 시 ragAvailable=false 반환
 *  - 호출부(TextToSqlService)에서 ragAvailable 체크 후 폴백 처리
 */
public class RagClient {

    private static final String RAG_SERVER_URL  = "http://127.0.0.1:8000/api/v1/search_context";
    private static final String HEALTH_URL      = "http://127.0.0.1:8000/health";

    /** 연결 타임아웃 — 이 시간 안에 TCP 연결 못 하면 폴백 */
    private static final int CONNECT_TIMEOUT_MS = 2000;

    /** 읽기 타임아웃 — 임베딩 + 검색 포함 전체 응답 허용 시간 */
    private static final int READ_TIMEOUT_MS    = 3000;

    /** 이 점수 미만이면 예제 없이 BASE 프롬프트만 사용 */
    private static final double SIMILARITY_THRESHOLD = 0.40;

    private RagClient() {}

    // ──────────────────────────────────────────────
    // 메인 인터페이스
    // ──────────────────────────────────────────────

    /**
     * RAG 서버에 질문을 보내고 유사 예제 + 카테고리를 반환.
     *
     * @param userQuestion 사용자 자연어 질문
     * @param topK         반환받을 예제 수
     * @return RagContextDTO (ragAvailable=false면 폴백 필요)
     */
    public static RagContextDTO searchContext(String userQuestion, int topK) {
        RagContextDTO result = new RagContextDTO();
        result.setRagAvailable(false); // 기본값: 실패

        try {
            // ① HTTP POST 요청 전송
            String requestBody = buildRequestJson(userQuestion, topK);
            String responseJson = post(RAG_SERVER_URL, requestBody);

            if (responseJson == null || responseJson.isEmpty()) {
                System.err.println("[RagClient] 빈 응답 수신 → 폴백");
                return result;
            }

            // ② JSON 파싱
            parseResponse(responseJson, result);

            // ③ 유사도 임계값 검사 — 전부 낮으면 예제 제거
            if (isBelowThreshold(result)) {
                System.out.println("[RagClient] 유사도 전부 " + SIMILARITY_THRESHOLD
                        + " 미만 → 예제 없이 BASE 프롬프트만 사용");
                result.setFewShotExamples(new ArrayList<>());
            }

            result.setRagAvailable(true);
            System.out.println("[RagClient] 검색 성공 — categories="
                    + result.getMatchedCategories()
                    + ", scores=" + result.getSimilarityScores());

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[RagClient] 타임아웃 → 폴백 (DynamicPromptBuilder)");
        } catch (java.net.ConnectException e) {
            System.err.println("[RagClient] RAG 서버 연결 불가 → 폴백");
        } catch (Exception e) {
            System.err.println("[RagClient] 오류 → 폴백: " + e.getMessage());
        }

        return result;
    }

    /**
     * RAG 서버 헬스체크.
     * TextToSqlService 초기화 시 1회 호출하여 서버 상태 확인.
     */
    public static boolean isHealthy() {
        try {
            URL url = new URL(HEALTH_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // HTTP POST
    // ──────────────────────────────────────────────
    private static String post(String endpoint, String body) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        int statusCode = conn.getResponseCode();
        if (statusCode != 200) {
            System.err.println("[RagClient] HTTP " + statusCode + " → 폴백");
            conn.disconnect();
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            conn.disconnect();
        }

        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // JSON 수동 조립
    // ──────────────────────────────────────────────
    private static String buildRequestJson(String question, int topK) {
        // 큰따옴표, 역슬래시, 개행 이스케이프
        String escaped = question
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "{\"query\":\"" + escaped + "\",\"top_k\":" + topK + "}";
    }

    // ──────────────────────────────────────────────
    // JSON 수동 파싱 (외부 라이브러리 없이)
    // ──────────────────────────────────────────────
    /**
     * Python 서버 응답 JSON을 파싱하여 RagContextDTO에 채움.
     *
     * 응답 예시:
     * {
     *   "matched_categories": ["ATTENDANCE", "EMPLOYEE"],
     *   "few_shot_examples": [
     *     {"question": "부장들 중 오늘 출근시간이 가장 빠른 사람", "sql": "SELECT ..."},
     *     {"question": "부서별 직급별 이번달 지각 횟수",           "sql": "SELECT ..."}
     *   ],
     *   "similarity_scores": [0.92, 0.85]
     * }
     */
    private static void parseResponse(String json, RagContextDTO result) {
        result.setMatchedCategories(parseStringArray(json, "matched_categories"));
        result.setSimilarityScores(parseDoubleArray(json, "similarity_scores"));
        result.setFewShotExamples(parseFewShotExamples(json));
    }

    /** JSON 배열에서 문자열 리스트 추출 — ["A","B"] 형태 */
    private static List<String> parseStringArray(String json, String key) {
        List<String> list = new ArrayList<>();
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) return list;

        int arrStart = json.indexOf('[', start);
        int arrEnd   = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return list;

        String arr = json.substring(arrStart + 1, arrEnd);
        for (String token : arr.split(",")) {
            String val = token.trim().replace("\"", "");
            if (!val.isEmpty()) list.add(val);
        }
        return list;
    }

    /** JSON 배열에서 double 리스트 추출 — [0.92, 0.85] 형태 */
    private static List<Double> parseDoubleArray(String json, String key) {
        List<Double> list = new ArrayList<>();
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) return list;

        int arrStart = json.indexOf('[', start);
        int arrEnd   = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return list;

        String arr = json.substring(arrStart + 1, arrEnd);
        for (String token : arr.split(",")) {
            token = token.trim();
            if (!token.isEmpty()) {
                try { list.add(Double.parseDouble(token)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    /**
     * few_shot_examples 배열 파싱.
     * 각 요소는 {"question": "...", "sql": "..."} 형태.
     * SQL에 큰따옴표가 없고 개행(\n)이 포함될 수 있음에 유의.
     */
    private static List<RagContextDTO.FewShotExample> parseFewShotExamples(String json) {
        List<RagContextDTO.FewShotExample> list = new ArrayList<>();

        int cursor = json.indexOf("\"few_shot_examples\"");
        if (cursor < 0) return list;

        int arrStart = json.indexOf('[', cursor);
        if (arrStart < 0) return list;

        // 중첩 괄호를 추적하여 배열 끝 찾기
        int depth    = 0;
        int arrEnd   = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') { depth--; if (depth == 0) { arrEnd = i; break; } }
        }
        if (arrEnd < 0) return list;

        String arrContent = json.substring(arrStart + 1, arrEnd);

        // 객체 단위로 분리
        int objDepth = 0;
        int objStart = -1;
        for (int i = 0; i < arrContent.length(); i++) {
            char c = arrContent.charAt(i);
            if (c == '{') {
                if (objDepth == 0) objStart = i;
                objDepth++;
            } else if (c == '}') {
                objDepth--;
                if (objDepth == 0 && objStart >= 0) {
                    String obj = arrContent.substring(objStart, i + 1);
                    RagContextDTO.FewShotExample ex = parseExample(obj);
                    if (ex != null) list.add(ex);
                    objStart = -1;
                }
            }
        }
        return list;
    }

    /** {"question":"...","sql":"..."} 단일 객체 파싱 */
    private static RagContextDTO.FewShotExample parseExample(String obj) {
        String question = extractStringValue(obj, "question");
        String sql      = extractStringValue(obj, "sql");
        if (question == null || sql == null) return null;

        RagContextDTO.FewShotExample ex = new RagContextDTO.FewShotExample();
        ex.setQuestion(question);
        ex.setSql(sql);
        return ex;
    }

    /**
     * JSON 객체에서 문자열 값 추출.
     * 이스케이프 시퀀스(\n, \", \\)를 실제 문자로 복원.
     */
    private static String extractStringValue(String obj, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = obj.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx  = obj.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int valStart = obj.indexOf('"', colonIdx + 1);
        if (valStart < 0) return null;

        // 닫는 따옴표 탐색 (이스케이프된 따옴표 건너뜀)
        int valEnd = valStart + 1;
        while (valEnd < obj.length()) {
            char c = obj.charAt(valEnd);
            if (c == '\\') { valEnd += 2; continue; } // 이스케이프 건너뜀
            if (c == '"')  { break; }
            valEnd++;
        }

        String raw = obj.substring(valStart + 1, valEnd);
        // 이스케이프 복원
        return raw
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    // ──────────────────────────────────────────────
    // 유사도 임계값 검사
    // ──────────────────────────────────────────────
    private static boolean isBelowThreshold(RagContextDTO result) {
        List<Double> scores = result.getSimilarityScores();
        if (scores == null || scores.isEmpty()) return true;
        for (double score : scores) {
            if (score >= SIMILARITY_THRESHOLD) return false;
        }
        return true;
    }
}
