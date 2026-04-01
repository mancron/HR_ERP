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
import com.hrms.sys.dto.TextToSqlResultDTO;
import com.hrms.sys.util.DynamicPromptBuilder;

import jakarta.servlet.http.HttpServlet;

public class TextToSqlService extends HttpServlet {

    // Ollama API 엔드포인트
    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "qwen2.5-coder:3b";

    // SELECT만 허용 — 화이트리스트
    private static final List<String> ALLOWED_START = Arrays.asList("SELECT", "select");

  
    // ──────────────────────────────────────
    // 메인: 텍스트 → SQL → 실행 → 결과 반환
    // ──────────────────────────────────────
    public TextToSqlResultDTO query(String userQuestion) {
        TextToSqlResultDTO result = new TextToSqlResultDTO();

        try {
            // 1. Ollama API 호출 → SQL 생성
            String rawSql = callOllama(userQuestion);
            String cleanSql = cleanSql(rawSql);
            result.setGeneratedSql(cleanSql);

            // 2. SELECT만 허용 검증
            if (!isSafeQuery(cleanSql)) {
                result.setErrorMsg("SELECT 쿼리만 실행할 수 있습니다.");
                return result;
            }

            // 3. DB 실행
            executeQuery(cleanSql, result);

        } catch (Exception e) {
            e.printStackTrace();
            result.setErrorMsg("처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    // ──────────────────────────────────────
    // Ollama REST API 호출
    // ──────────────────────────────────────
    private String callOllama(String userQuestion) throws IOException {
    	String prompt = DynamicPromptBuilder.build(userQuestion);

    	System.out.println("[AI] 매칭 카테고리: " + DynamicPromptBuilder.getMatchedCategories(userQuestion));
        System.out.println("[AI] 프롬프트 토큰: ~" + DynamicPromptBuilder.estimateTokens(prompt));
        // JSON 요청 본문 수동 조립 (외부 라이브러리 없이)
        String requestBody = "{"
            + "\"model\":\"" + OLLAMA_MODEL + "\","
            + "\"prompt\":\"" + escapeJson(prompt) + "\","
            + "\"stream\":false"
            + "}";

        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000); // 모델 추론 시간 고려 (60초)

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("Ollama API 응답 오류: " + conn.getResponseCode());
        }

        // 응답 읽기
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        // JSON에서 "response" 필드 추출 (라이브러리 없이 간단 파싱)
        String response = sb.toString();
        return extractResponseField(response);
    }

    // ──────────────────────────────────────
    // DB 실행 및 결과 매핑
    // ──────────────────────────────────────
    private void executeQuery(String sql, TextToSqlResultDTO result) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
        	conn = DatabaseConnection.getReadOnlyConnection();
            stmt = conn.createStatement();
            stmt.setMaxRows(500); // 최대 500행 제한

            rs = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // 컬럼명 추출
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            result.setColumns(columns);

            // 데이터 추출
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
            if (rs   != null) try { rs.close();   } catch (SQLException e) {}
            if (stmt != null) try { stmt.close();  } catch (SQLException e) {}
            if (conn != null) try { conn.close();  } catch (SQLException e) {}
        }
    }

    // ──────────────────────────────────────
    // Private 유틸
    // ──────────────────────────────────────

    /** 코드블록 및 공백 제거 */
    private String cleanSql(String raw) {
        return raw.replaceAll("(?i)```sql", "")
                  .replaceAll("```", "")
                  .replaceAll("\\n", " ")
                  .trim();
    }

    /** SELECT로 시작하는지 검증 */
    private boolean isSafeQuery(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("SELECT");
    }

    /** JSON "response" 필드 간단 파싱 */
    private String extractResponseField(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        String result = json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"");

        // 유니코드 디코딩 추가 (\u003e → > 등)
        return decodeUnicode(result);
    }
    
    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length()
                    && input.charAt(i) == '\\'
                    && input.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(input.charAt(i));
                    i++;
                }
            } else {
                sb.append(input.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /** JSON 문자열 이스케이프 */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}