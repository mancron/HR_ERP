package com.hrms.sys.service;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dto.TextToSqlResultDTO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class TextToSqlService {

    // Ollama API 엔드포인트
    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "qwen2.5-coder:3b";

    // SELECT만 허용 — 화이트리스트
    private static final List<String> ALLOWED_START = Arrays.asList("SELECT", "select");

    // 스키마 프롬프트 (모델에게 DB 구조 주입)
    // =============================================
    // SCHEMA_PROMPT v2 — 최적화 버전
    // =============================================
    // 개선사항:
    //   1. 토큰 효율: 압축 표기법 사용 (~30% 절감)
    //   2. 커버리지: 집계/날짜/서브쿼리/NULL/LIKE 패턴 추가
    //   3. 한국어 동의어 매핑: 자연어 → SQL 변환 정확도 향상
    //   4. 안전장치: 환각 방지, 모호성 대응 규칙 강화
    //   5. 뷰 우선 전략: 불필요한 JOIN 방지
    // =============================================
 
    private static final String SCHEMA_PROMPT =
        "You are a MySQL expert. Generate ONLY a raw MySQL SELECT query.\n\n" +
 
        // ── 출력 규칙 ──
        "OUTPUT RULES:\n" +
        "- Output ONLY the SQL query. No markdown, no explanation, no code blocks.\n" +
        "- End with semicolon.\n" +
        "- Use ONLY columns explicitly listed below. NEVER invent columns.\n" +
        "- Always use PK/FK relationships for JOINs.\n" +
        "- Default LIMIT 100 unless user specifies count.\n" +
        "- For ambiguous questions, return broader results rather than guessing filters.\n\n" +
 
        // ── 한국어 → SQL 도메인 매핑 ──
        "DOMAIN RULES (Korean → SQL):\n" +
        "Status Filters:\n" +
        "  재직자/현재직원/일하는사람: employee.status='재직'\n" +
        "  퇴사자/퇴직자/그만둔사람: employee.status='퇴직'\n" +
        "  휴직자/쉬는사람: employee.status='휴직'\n" +
        "  정규직/계약직/파트타임: employee.emp_type='정규직'|'계약직'|'파트타임'\n" +
        "  잠긴계정/잠금: account.locked_at IS NOT NULL\n" +
        "  활성계정: account.is_active=1 AND account.locked_at IS NULL\n" +
        "  운영중인부서/직급: is_active=1\n" +
        "  폐지된부서/직급: is_active=0\n" +
        "Approval Status:\n" +
        "  승인된/처리된: status='승인'\n" +
        "  대기중/미승인/결재대기: status='대기'\n" +
        "  반려된/거절된: status='반려'\n" +
        "  취소된: status='취소'\n" +
        "Salary:\n" +
        "  연봉/기준연봉/기본연봉: employee.base_salary (월기본급 아님, 연봉 기준)\n" +
        "  월급/급여/실수령: salary table (net_salary=실수령, gross_salary=세전)\n" +
        "  지급완료: salary.status='완료'\n" +
        "  급여대기/미지급: salary.status='대기'\n" +
        "Evaluation:\n" +
        "  확정된평가: evaluation.eval_status='최종확정'\n" +
        "  작성중평가: evaluation.eval_status='작성중'\n" +
        "Notification:\n" +
        "  안읽은알림/새알림: notification.is_read=0\n" +
        "  읽은알림: notification.is_read=1\n" +
        "Organization:\n" +
        "  부서장/팀장: department.manager_id=employee.emp_id\n" +
        "  상위부서: department.parent_dept_id\n" +
        "Age:\n" +
        "  나이/만나이: TIMESTAMPDIFF(YEAR, employee.birth_date, CURDATE()). NEVER use 'age' column.\n" +
        "  근속연수/재직기간: TIMESTAMPDIFF(YEAR, employee.hire_date, CURDATE())\n" +
        "  근속월수: TIMESTAMPDIFF(MONTH, employee.hire_date, CURDATE())\n" +
        "Time:\n" +
        "  이번달/당월: YEAR(CURDATE()), MONTH(CURDATE())\n" +
        "  지난달/전월: YEAR(DATE_SUB(CURDATE(),INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(),INTERVAL 1 MONTH))\n" +
        "  올해/금년: YEAR(CURDATE())\n" +
        "  작년/전년: YEAR(CURDATE())-1\n" +
        "  상반기: eval_period='상반기' or MONTH BETWEEN 1 AND 6\n" +
        "  하반기: eval_period='하반기' or MONTH BETWEEN 7 AND 12\n\n" +
 
        // ── 스키마 정의 (압축 표기) ──
        "SCHEMA (db: hr_erp):\n" +
        "Tables:\n" +
        "  job_position(PK:position_id, position_name, position_level(1~5), base_salary, meal_allowance, transport_allowance, position_allowance, is_active(1/0), created_at)\n" +
        "  department(PK:dept_id, dept_name, FK:parent_dept_id→department, FK:manager_id→employee, dept_level, sort_order, is_active(1/0), closed_at, created_at)\n" +
        "  employee(PK:emp_id, emp_name, emp_no, FK:dept_id, FK:position_id, hire_date, resign_date, emp_type ENUM('정규직','계약직','파트타임'), status ENUM('재직','휴직','퇴직'), base_salary, birth_date, gender ENUM('M','F'), address, emergency_contact, bank_account, email, phone, created_at)\n" +
        "  personnel_history(PK:history_id, FK:emp_id, change_type ENUM('발령','승진','전보','퇴직','복직'), FK:from_dept_id→dept, FK:to_dept_id→dept, FK:from_position_id→job_position, FK:to_position_id→job_position, change_date, reason, FK:approved_by→employee, created_at)\n" +
        "  account(PK:account_id, FK:emp_id, username, role ENUM('관리자','HR담당자','일반'), is_active(1/0), login_attempts, last_login, password_changed_at, locked_at, created_at)\n" +
        "  attendance(PK:att_id, FK:emp_id, work_date, check_in, check_out, work_hours, overtime_hours, status ENUM('출근','지각','결근','휴가','출장'), note, created_at)\n" +
        "  leave_request(PK:leave_id, FK:emp_id, leave_type ENUM('연차','반차','병가','경조사','공가'), half_type ENUM('오전','오후'), start_date, end_date, days, reason, status ENUM('대기','승인','반려','취소'), FK:approver_id→employee, approved_at, reject_reason, created_at)\n" +
        "  annual_leave(PK:al_id, FK:emp_id, leave_year, total_days, used_days, remain_days)\n" +
        "  overtime_request(PK:ot_id, FK:emp_id, ot_date, start_time, end_time, ot_hours, reason, status ENUM('대기','승인','반려'), FK:approver_id→employee, approved_at, created_at)\n" +
        "  public_holiday(PK:holiday_id, holiday_date, holiday_name, holiday_year, created_at)\n" +
        "  salary(PK:salary_id, FK:emp_id, salary_year, salary_month, base_salary, meal_allowance, transport_allowance, position_allowance, overtime_pay, other_allowance, gross_salary, national_pension, health_insurance, long_term_care, employment_insurance, income_tax, local_income_tax, total_deduction, net_salary, pay_date, status ENUM('대기','완료'), created_at)\n" +
        "  deduction_rate(PK:rate_id, target_year, national_pension_rate, health_insurance_rate, long_term_care_rate, employment_insurance_rate, created_at)\n" +
        "  evaluation(PK:eval_id, FK:emp_id, eval_year, eval_period ENUM('상반기','하반기','연간'), eval_type ENUM('자기평가','상위평가','동료평가'), total_score, grade ENUM('S','A','B','C','D'), eval_comment, eval_status ENUM('작성중','최종확정'), FK:evaluator_id→employee, confirmed_at, created_at)\n" +
        "  evaluation_item(PK:item_id, FK:eval_id, item_name, score, max_score)\n" +
        "  notification(PK:noti_id, FK:emp_id, noti_type, ref_table, ref_id, message, is_read(1/0), read_at, created_at)\n" +
        "  audit_log(PK:log_id, FK:actor_id→employee, target_table, target_id, action ENUM('INSERT','UPDATE','DELETE'), column_name, old_value, new_value, created_at)\n\n" +
 
        // ── 뷰 정의 ──
        "PRE-BUILT VIEWS (prefer views over raw JOINs when columns match):\n" +
        "  v_employee_full(emp_id, emp_name, emp_no, status, emp_type, base_salary, hire_date, resign_date, birth_date, gender, email, phone, dept_name, position_name, position_level)\n" +
        "  v_salary_summary(emp_id, emp_name, emp_no, dept_name, position_name, salary_year, salary_month, base_salary, gross_salary, total_deduction, net_salary, overtime_pay, salary_status, pay_date)\n" +
        "  v_leave_status(emp_id, emp_name, emp_no, dept_name, position_name, leave_id, leave_type, half_type, start_date, end_date, days, reason, leave_status, approved_at, reject_reason, leave_year, total_days, used_days, remain_days)\n" +
        "  v_evaluation_result(emp_id, emp_name, emp_no, dept_name, position_name, eval_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, confirmed_at, evaluator_name)\n\n" +
 
        // ── SQL 패턴 가이드 ──
        "SQL PATTERNS:\n" +
        "- Count/통계: SELECT COUNT(*), AVG(), SUM(), MIN(), MAX() with GROUP BY\n" +
        "- Ranking: ORDER BY col DESC LIMIT N\n" +
        "- 부서별/직급별 통계: GROUP BY dept_name or position_name (use views)\n" +
        "- 기간 범위: WHERE date_col BETWEEN 'YYYY-MM-DD' AND 'YYYY-MM-DD'\n" +
        "- 이름 검색/포함: WHERE emp_name LIKE '%keyword%'\n" +
        "- NULL 체크: IS NULL / IS NOT NULL (locked_at, resign_date, etc.)\n" +
        "- 서브쿼리: Use for '가장 높은', '가장 많은', '평균 이상' patterns\n" +
        "- 다중 테이블: When view columns are insufficient, JOIN raw tables\n" +
        "- Percentage: ROUND(count/total*100,1) for 비율/퍼센트\n" +
        "- Year-Month grouping: GROUP BY salary_year, salary_month\n\n" +
 
        // ── Few-shot 예제 (패턴별 대표) ──
        "EXAMPLES:\n" +
        "Q: 재직 중인 직원들의 부서명 조회\n" +
        "A: SELECT emp_name, dept_name FROM v_employee_full WHERE status = '재직';\n\n" +
        "Q: 홍길동이 어느 부서의 어떤 직급이야?\n" +
        "A: SELECT emp_name, dept_name, position_name FROM v_employee_full WHERE emp_name = '홍길동';\n\n" +
        "Q: 잠긴 계정 목록을 보여줘\n" +
        "A: SELECT a.account_id, e.emp_name, a.username, a.locked_at FROM account a JOIN employee e ON a.emp_id = e.emp_id WHERE a.locked_at IS NOT NULL;\n\n" +
        "Q: 부서별 직원 수\n" +
        "A: SELECT dept_name, COUNT(*) AS emp_count FROM v_employee_full WHERE status = '재직' GROUP BY dept_name ORDER BY emp_count DESC;\n\n" +
        "Q: 연봉이 가장 높은 직원 3명\n" +
        "A: SELECT emp_name, dept_name, position_name, base_salary FROM v_employee_full WHERE status = '재직' ORDER BY base_salary DESC LIMIT 3;\n\n" +
        "Q: 인사팀 이번달 급여 현황\n" +
        "A: SELECT emp_name, gross_salary, total_deduction, net_salary, salary_status FROM v_salary_summary WHERE dept_name = '인사팀' AND salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE());\n\n" +
        "Q: 2024년 하반기 S등급 받은 직원\n" +
        "A: SELECT emp_name, dept_name, total_score, grade FROM v_evaluation_result WHERE eval_year = 2024 AND eval_period = '하반기' AND grade = 'S';\n\n" +
        "Q: 30세 이상 40세 이하 재직 직원\n" +
        "A: SELECT emp_name, dept_name, position_name, TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS age FROM v_employee_full WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) BETWEEN 30 AND 40;\n\n" +
        "Q: 부서별 평균 연봉\n" +
        "A: SELECT dept_name, ROUND(AVG(base_salary)) AS avg_salary, COUNT(*) AS emp_count FROM v_employee_full WHERE status = '재직' GROUP BY dept_name ORDER BY avg_salary DESC;\n\n" +
        "Q: 이번달 지각한 사람\n" +
        "A: SELECT e.emp_name, a.work_date, a.check_in FROM attendance a JOIN employee e ON a.emp_id = e.emp_id WHERE a.status = '지각' AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE());\n\n" +
        "Q: 연차 남은 일수가 3일 이하인 직원\n" +
        "A: SELECT emp_name, dept_name, remain_days FROM v_leave_status WHERE leave_year = YEAR(CURDATE()) AND remain_days <= 3 GROUP BY emp_id, emp_name, dept_name, remain_days;\n\n" +
        "Q: 김으로 시작하는 직원 목록\n" +
        "A: SELECT emp_name, dept_name, position_name FROM v_employee_full WHERE emp_name LIKE '김%' AND status = '재직';\n\n" +
        "Q: 근속 5년 이상인 직원\n" +
        "A: SELECT emp_name, dept_name, hire_date, TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) AS years_worked FROM v_employee_full WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) >= 5 ORDER BY hire_date;\n\n" +
        "Q: 이번 달 초과근무 승인된 건수와 총 시간\n" +
        "A: SELECT COUNT(*) AS approved_count, SUM(ot_hours) AS total_ot_hours FROM overtime_request WHERE status = '승인' AND YEAR(ot_date) = YEAR(CURDATE()) AND MONTH(ot_date) = MONTH(CURDATE());\n\n" +
        "Q: 직급별 인원수와 평균연봉\n" +
        "A: SELECT position_name, COUNT(*) AS cnt, ROUND(AVG(base_salary)) AS avg_salary FROM v_employee_full WHERE status = '재직' GROUP BY position_name, position_level ORDER BY position_level;\n\n" +
        "Q: 퇴직자 중 올해 퇴직한 사람\n" +
        "A: SELECT emp_name, dept_name, resign_date FROM v_employee_full WHERE status = '퇴직' AND YEAR(resign_date) = YEAR(CURDATE());\n\n" +
        "Q: 2025년 공휴일 목록\n" +
        "A: SELECT holiday_date, holiday_name FROM public_holiday WHERE holiday_year = 2025 ORDER BY holiday_date;\n\n" +
        "Q: 최근 감사로그 10건\n" +
        "A: SELECT al.created_at, e.emp_name AS actor, al.target_table, al.action, al.column_name, al.old_value, al.new_value FROM audit_log al LEFT JOIN employee e ON al.actor_id = e.emp_id ORDER BY al.created_at DESC LIMIT 10;\n\n" +
        "Q: 성별 비율\n" +
        "A: SELECT gender, COUNT(*) AS cnt, ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM employee WHERE status='재직'),1) AS pct FROM employee WHERE status = '재직' GROUP BY gender;\n\n" +
        "Q: 평균 연봉보다 높은 직원\n" +
        "A: SELECT emp_name, dept_name, base_salary FROM v_employee_full WHERE status = '재직' AND base_salary > (SELECT AVG(base_salary) FROM employee WHERE status = '재직') ORDER BY base_salary DESC;\n\n" +
 
        "Question: ";

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
        String prompt = SCHEMA_PROMPT + userQuestion;

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