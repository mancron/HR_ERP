package com.hrms.sys.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.RagSuccessLogDAO;
import com.hrms.sys.dto.RagContextDTO;
import com.hrms.sys.dto.RagSuccessLogDTO;
import com.hrms.sys.dto.TextToSqlResultDTO;
import com.hrms.sys.util.DynamicPromptBuilder;
import com.hrms.sys.util.PromptAssembler;
import com.hrms.sys.util.RagClient;

import jakarta.servlet.http.HttpServlet;

/**
 * TextToSqlService v2 — RAG 통합 버전
 *
 * 변경점 (기존 v1 대비):
 *   [1] callOllama() 앞에 RagClient.searchContext() 추가
 *   [2] RAG 성공 → PromptAssembler.assemble()로 프롬프트 조립
 *   [3] RAG 실패 → PromptAssembler.assembleFallback() (기존 DynamicPromptBuilder)
 *   [4] 나머지 로직(안전 검증, DB 실행, 결과 반환)은 기존과 동일
 */
public class TextToSqlService extends HttpServlet {

    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "qwen2.5-coder:3b";

    private static final List<String> ALLOWED_START = Arrays.asList("SELECT", "select");
    private final RagSuccessLogDAO ragLogDao = new RagSuccessLogDAO();
    
    // RAG 검색 예제 수
    private static final int RAG_TOP_K = 2;

    // ──────────────────────────────────────────────
    // 메인: 텍스트 → SQL → 실행 → 결과 반환
    // ──────────────────────────────────────────────
    public TextToSqlResultDTO query(String userQuestion) {
        TextToSqlResultDTO result    = new TextToSqlResultDTO();
        RagContextDTO      ragContext = null;  // 로그 저장용으로 보관

        try {
            // ① RAG 호출 (프롬프트 조립을 위해 ragContext를 분리하여 보관)
            ragContext = RagClient.searchContext(userQuestion, RAG_TOP_K);

            String prompt;
            if (ragContext.isRagAvailable()) {
                prompt = PromptAssembler.assemble(ragContext, userQuestion);
                System.out.println("[TextToSqlService] RAG 모드 — categories="
                        + ragContext.getMatchedCategories()
                        + ", 토큰=~" + DynamicPromptBuilder.estimateTokens(prompt));
            } else {
                prompt = PromptAssembler.assembleFallback(userQuestion);
                System.out.println("[TextToSqlService] 폴백 모드 — categories="
                        + DynamicPromptBuilder.getMatchedCategories(userQuestion)
                        + ", 토큰=~" + DynamicPromptBuilder.estimateTokens(prompt));
            }

            // ② Ollama 호출
            String rawSql   = callOllama(prompt);
            String cleanSql = cleanSql(rawSql);
            result.setGeneratedSql(cleanSql);

            // ③ 불가능 응답 처리
            if ("불가능".equals(cleanSql.trim())) {
                result.setErrorMsg("해당 질문은 현재 HR 데이터베이스 스키마로 조회할 수 없습니다.");
                return result;
            }

            // ④ SELECT 화이트리스트 검증
            if (!isSafeQuery(cleanSql)) {
                result.setErrorMsg("SELECT 쿼리만 실행할 수 있습니다.");
                return result;
            }

            // ⑤ DB 실행
            executeQuery(cleanSql, result);

            // ⑥ 성공 로그 저장 (결과 행 수 > 0인 경우만)
            //    메인 트랜잭션과 격리된 별도 try-catch로 처리
            if (result.getErrorMsg() == null && result.getRows() != null
                    && !result.getRows().isEmpty()) {
                saveSuccessLog(userQuestion, cleanSql, ragContext, result.getRows().size());
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.setErrorMsg("처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }
    
    private void saveSuccessLog(String question, String sql,
            RagContextDTO ragContext, int rowCount) {
		try (Connection conn = DatabaseConnection.getConnection()) {
		RagSuccessLogDTO logDto = new RagSuccessLogDTO();
		logDto.setQuestion    (question);
		logDto.setGeneratedSql(sql);
		logDto.setRowCount    (rowCount);
		
		if (ragContext != null && ragContext.isRagAvailable()) {
		// RAG 경로
		logDto.setUsedRag (true);
		logDto.setCategory(String.join(",", ragContext.getMatchedCategories()));
		List<Double> scores = ragContext.getSimilarityScores();
		if (scores != null && !scores.isEmpty()) {
		logDto.setSimilarity(scores.get(0)); // 최고 유사도
		}
		} else {
		// 폴백 경로
		logDto.setUsedRag (false);
		logDto.setCategory(DynamicPromptBuilder.getMatchedCategories(question));
		logDto.setSimilarity(null);
		}
		
		ragLogDao.insert(conn, logDto);
		
		} catch (Exception e) {
		// 로그 저장 실패는 메인 기능에 영향 주지 않음
		System.err.println("[TextToSqlService] 성공 로그 저장 실패 (무시): " + e.getMessage());
		}
    }

    
    
    // ──────────────────────────────────────────────
    // 프롬프트 조립 (RAG → 폴백)
    // ──────────────────────────────────────────────
    private String buildPrompt(String userQuestion) {

        // RAG 서버 호출
        RagContextDTO ragContext = RagClient.searchContext(userQuestion, RAG_TOP_K);

        if (ragContext.isRagAvailable()) {
            // 정상 경로: RAG 결과로 프롬프트 조립
            String prompt = PromptAssembler.assemble(ragContext, userQuestion);
            System.out.println("[TextToSqlService] RAG 모드 — categories="
                    + ragContext.getMatchedCategories()
                    + ", 예제=" + ragContext.getFewShotExamples().size() + "개"
                    + ", 토큰=~" + DynamicPromptBuilder.estimateTokens(prompt));
            return prompt;
        }

        // 폴백: 기존 DynamicPromptBuilder 키워드 매칭
        String prompt = PromptAssembler.assembleFallback(userQuestion);
        System.out.println("[TextToSqlService] 폴백 모드 — categories="
                + DynamicPromptBuilder.getMatchedCategories(userQuestion)
                + ", 토큰=~" + DynamicPromptBuilder.estimateTokens(prompt));
        return prompt;
    }

    // ──────────────────────────────────────────────
    // Ollama REST API 호출 (기존과 동일)
    // ──────────────────────────────────────────────
    private String callOllama(String prompt) throws IOException {
        String requestBody = "{"
                + "\"model\":\""  + OLLAMA_MODEL + "\","
                + "\"prompt\":\"" + escapeJson(prompt) + "\","
                + "\"stream\":false,"
                + "\"options\":{"
                +   "\"temperature\":0,"
                +   "\"top_p\":1,"
                +   "\"repeat_penalty\":1.1,"
                +   "\"num_predict\":300"
                + "}"
                + "}";

        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("Ollama API 응답 오류: " + conn.getResponseCode());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return extractResponseField(sb.toString());
    }

    // ──────────────────────────────────────────────
    // DB 실행 (기존과 동일 — ReadOnly Connection 사용)
    // ──────────────────────────────────────────────
    private void executeQuery(String sql, TextToSqlResultDTO result) throws SQLException {
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;
        try {
            conn = DatabaseConnection.getReadOnlyConnection();
            stmt = conn.createStatement();
            stmt.setMaxRows(500);

            rs = stmt.executeQuery(sql);
            ResultSetMetaData meta     = rs.getMetaData();
            int               colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnLabel(i));
            result.setColumns(columns);

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    row.put(meta.getColumnLabel(i), val != null ? val.toString() : "");
                }
                rows.add(row);
            }
            result.setRows(rows);
            result.setRowCount(rows.size());

        } finally {
            if (rs   != null) try { rs.close();   } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close();  } catch (SQLException ignored) {}
            if (conn != null) try { conn.close();  } catch (SQLException ignored) {}
        }
    }

    // ──────────────────────────────────────────────
    // 유틸 (기존과 동일)
    // ──────────────────────────────────────────────
    private boolean isSafeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) return false;
        String upper = sql.trim().toUpperCase();
        for (String prefix : ALLOWED_START) {
            if (upper.startsWith(prefix.toUpperCase())) return true;
        }
        return false;
    }

    private String cleanSql(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("```sql", "")
                  .replaceAll("```", "")
                  .replaceAll("(?i)^sql\\s*", "")
                  .trim();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractResponseField(String json) {
        String key    = "\"response\"";
        int    start  = json.indexOf(key);
        if (start < 0) return "";
        int valStart  = json.indexOf('"', start + key.length() + 1);
        if (valStart < 0) return "";
        int valEnd    = valStart + 1;
        while (valEnd < json.length()) {
            char c = json.charAt(valEnd);
            if (c == '\\') { valEnd += 2; continue; }
            if (c == '"')  { break; }
            valEnd++;
        }
        return json.substring(valStart + 1, valEnd)
                   .replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\")
                   .replace("\\u003e", ">")
                   .replace("\\u003c", "<")
                   .replace("\\u0026", "&");
    }
}
