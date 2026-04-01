package com.hrms.sys.util;

import java.util.*;

/**
 * 동적 프롬프트 빌더 v2 — 3B 모델 최적화 (컬럼 혼동 방지 강화)
 *
 * 해결하는 문제:
 *   1. "관리자 목록" → role은 account에만 있는데 v_employee_full에서 찾음
 *   2. "부장 목록"   → position_name 대신 emp_type 사용
 *   3. "부서장 목록" → manager_id는 department에만 있는데 뷰에서 찾음
 *   4. "나이/30대"   → age 컬럼이 없는데 사용
 *   5. "급여 지급완료" → 뷰에서 status 대신 salary_status 사용해야 함
 *   6. dept_id가 v_employee_full에 없는데 JOIN 시도
 *   7. "성별" → gender='남자' 대신 'M'/'F' 사용
 *   8. evaluator_name은 v_evaluation_result에만 있음
 *
 * 사용법:
 *   String prompt = DynamicPromptBuilder.build("관리자 목록");
 */
public class DynamicPromptBuilder {

    // ================================================================
    // 1. 카테고리 정의
    // ================================================================
    private enum Category {
        EMPLOYEE, SALARY, ATTENDANCE, LEAVE, EVALUATION, ACCOUNT, SYSTEM
    }

    // ================================================================
    // 2. 카테고리별 키워드 매핑
    // ================================================================
    private static final Map<Category, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(Category.EMPLOYEE, Arrays.asList(
            // 직급명 (가장 자주 혼동되는 패턴)
            "사원", "대리", "과장", "차장", "부장",
            // 직원 상태
            "재직", "퇴직", "휴직", "퇴사", "복직",
            // 고용 형태
            "정규직", "계약직", "파트타임",
            // 조회 패턴
            "직원", "인원", "명단", "리스트",
            // 인적사항
            "성별", "나이", "만나이", "생년", "근속", "남자", "여자", "남성", "여성",
            // 검색/집계
            "이름", "검색", "누구", "부서별", "직급별",
            // 연봉 (employee.base_salary)
            "연봉", "기본급", "기준연봉",
            // 조직
            "부서장", "팀장", "부서", "조직",
            // 입사/경력
            "입사", "경력", "신입",
            // 인사발령
            "발령", "승진", "전보", "인사이동"
        ));

        KEYWORDS.put(Category.SALARY, Arrays.asList(
            "급여", "월급", "실수령", "세전", "세후",
            "공제", "수당", "식대", "교통비", "직책수당",
            "지급", "미지급", "급여대기", "명세",
            "국민연금", "건강보험", "고용보험", "소득세",
            "공제율", "보험료"
        ));

        KEYWORDS.put(Category.ATTENDANCE, Arrays.asList(
            "출근", "퇴근", "지각", "결근", "출장",
            "근태", "출퇴근", "근무", "근무시간",
            "초과근무", "야근", "잔업", "오버타임"
        ));

        KEYWORDS.put(Category.LEAVE, Arrays.asList(
            "휴가", "연차", "반차", "병가", "경조사", "공가",
            "잔여", "남은", "사용", "휴가일수",
            "휴가신청", "휴가승인", "휴가반려"
        ));

        KEYWORDS.put(Category.EVALUATION, Arrays.asList(
            "평가", "인사평가", "고과", "등급",
            "S등급", "A등급", "B등급", "C등급", "D등급",
            "상반기", "하반기", "연간",
            "점수", "자기평가", "상위평가", "동료평가",
            "확정", "작성중", "평가자"
        ));

        KEYWORDS.put(Category.ACCOUNT, Arrays.asList(
            "계정", "잠금", "잠긴", "로그인", "로그아웃",
            "권한", "관리자", "HR담당자", "비밀번호",
            "활성", "비활성", "차단", "아이디", "username"
        ));

        KEYWORDS.put(Category.SYSTEM, Arrays.asList(
            "감사", "로그", "이력", "변경이력",
            "알림", "안읽은", "새알림", "읽은알림",
            "공휴일", "휴일", "명절"
        ));
    }

    // ================================================================
    // 3. BASE 프롬프트 (항상 포함)
    //    - 핵심 경고를 최상단에 배치 (3B 모델의 attention 집중 영역)
    // ================================================================
    private static final String BASE_PROMPT =
        "You are a MySQL expert. Generate ONLY a raw MySQL SELECT query.\n" +
        "Output ONLY the SQL. No markdown, no explanation. End with semicolon.\n" +
        "Use ONLY columns listed below. NEVER invent columns.\n" +
        "Default LIMIT 100.\n\n" +

        // ── 가장 중요한 경고를 맨 위에 배치 ──
        "CRITICAL COLUMN WARNINGS:\n" +
        "- role column: ONLY in account table. Views do NOT have role.\n" +
        "- position_name: ONLY in views and job_position. NEVER use emp_type for 부장/과장/대리/사원.\n" +
        "- emp_type: ONLY for 정규직/계약직/파트타임. NEVER for 부장/과장/대리/사원.\n" +
        "- manager_id: ONLY in department table. Views do NOT have manager_id.\n" +
        "- age column: does NOT exist. Use TIMESTAMPDIFF(YEAR, birth_date, CURDATE()).\n" +
        "- gender: ENUM('M','F'). NEVER use '남자','여자','남','여'.\n" +
        "- v_employee_full does NOT have: dept_id, role, manager_id, locked_at.\n" +
        "- v_salary_summary uses salary_status (NOT status) for 지급상태.\n" +
        "- v_leave_status uses leave_status (NOT status) for 휴가상태.\n\n" +

        "VIEWS (use when all needed columns exist):\n" +
        "  v_employee_full(emp_id, emp_name, emp_no, status, emp_type, base_salary, hire_date, resign_date, birth_date, gender, email, phone, dept_name, position_name, position_level)\n" +
        "  v_salary_summary(emp_id, emp_name, emp_no, dept_name, position_name, salary_year, salary_month, base_salary, gross_salary, total_deduction, net_salary, overtime_pay, salary_status, pay_date)\n" +
        "  v_leave_status(emp_id, emp_name, emp_no, dept_name, position_name, leave_id, leave_type, half_type, start_date, end_date, days, reason, leave_status, approved_at, reject_reason, leave_year, total_days, used_days, remain_days)\n" +
        "  v_evaluation_result(emp_id, emp_name, emp_no, dept_name, position_name, eval_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, confirmed_at, evaluator_name)\n\n";

    // ================================================================
    // 4. 카테고리별 프롬프트 조각
    //    각 조각에 "이 카테고리에서 자주 틀리는 패턴" 경고를 포함
    // ================================================================
    private static final Map<Category, String> CATEGORY_PROMPTS = new LinkedHashMap<>();

    static {
        // ──────────────────────────────────────────
        // EMPLOYEE
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.EMPLOYEE,
            "TABLES:\n" +
            "  employee(PK:emp_id, emp_name, emp_no, FK:dept_id, FK:position_id, hire_date, resign_date, emp_type ENUM('정규직','계약직','파트타임'), status ENUM('재직','휴직','퇴직'), base_salary, birth_date, gender ENUM('M','F'), email, phone)\n" +
            "  job_position(PK:position_id, position_name, position_level(1=사원~5=부장), base_salary, is_active)\n" +
            "  department(PK:dept_id, dept_name, FK:parent_dept_id, FK:manager_id→employee, dept_level, is_active)\n" +
            "  personnel_history(PK:history_id, FK:emp_id, change_type ENUM('발령','승진','전보','퇴직','복직'), FK:from_dept_id, FK:to_dept_id, FK:from_position_id, FK:to_position_id, change_date, reason)\n\n" +

            "RULES:\n" +
            "  부장/차장/과장/대리/사원 → WHERE position_name='부장' on v_employee_full\n" +
            "  정규직/계약직/파트타임 → WHERE emp_type='정규직'\n" +
            "  재직/퇴직/휴직 → WHERE status='재직'\n" +
            "  나이 → TIMESTAMPDIFF(YEAR, birth_date, CURDATE())\n" +
            "  근속 → TIMESTAMPDIFF(YEAR, hire_date, CURDATE())\n" +
            "  성별 → gender='M' (남), gender='F' (여). NEVER '남자','여자'\n" +
            "  부서장 → SELECT e.emp_name FROM department d JOIN employee e ON d.manager_id=e.emp_id\n" +
            "  부서별통계 → GROUP BY dept_name (use v_employee_full)\n\n" +

            "EXAMPLES:\n" +
            "Q: 부장 목록\n" +
            "A: SELECT emp_name, dept_name, position_name, email, phone FROM v_employee_full WHERE position_name = '부장' AND status = '재직';\n\n" +
            "Q: 재직 중인 직원 부서명 조회\n" +
            "A: SELECT emp_name, dept_name FROM v_employee_full WHERE status = '재직';\n\n" +
            "Q: 부서별 직원 수\n" +
            "A: SELECT dept_name, COUNT(*) AS emp_count FROM v_employee_full WHERE status = '재직' GROUP BY dept_name ORDER BY emp_count DESC;\n\n" +
            "Q: 과장 이상 직원\n" +
            "A: SELECT emp_name, dept_name, position_name FROM v_employee_full WHERE position_level >= 3 AND status = '재직' ORDER BY position_level DESC;\n\n" +
            "Q: 부서장 목록\n" +
            "A: SELECT e.emp_name, d.dept_name, p.position_name FROM department d JOIN employee e ON d.manager_id = e.emp_id JOIN job_position p ON e.position_id = p.position_id WHERE d.is_active = 1;\n\n" +
            "Q: 성별 비율\n" +
            "A: SELECT gender, COUNT(*) AS cnt, ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM employee WHERE status='재직'),1) AS pct FROM employee WHERE status = '재직' GROUP BY gender;\n\n" +
            "Q: 30세 이상 40세 이하 재직 직원\n" +
            "A: SELECT emp_name, dept_name, TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS age FROM v_employee_full WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) BETWEEN 30 AND 40;\n\n" +
            "Q: 근속 5년 이상 직원\n" +
            "A: SELECT emp_name, dept_name, hire_date, TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) AS years_worked FROM v_employee_full WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) >= 5 ORDER BY hire_date;\n\n" +
            "Q: 연봉이 가장 높은 직원 3명\n" +
            "A: SELECT emp_name, dept_name, position_name, base_salary FROM v_employee_full WHERE status = '재직' ORDER BY base_salary DESC LIMIT 3;\n\n" +
            "Q: 김으로 시작하는 직원\n" +
            "A: SELECT emp_name, dept_name, position_name FROM v_employee_full WHERE emp_name LIKE '김%' AND status = '재직';\n\n" +
            "Q: 퇴직자 중 올해 퇴직한 사람\n" +
            "A: SELECT emp_name, dept_name, resign_date FROM v_employee_full WHERE status = '퇴직' AND YEAR(resign_date) = YEAR(CURDATE());\n\n"
        );

        // ──────────────────────────────────────────
        // SALARY
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.SALARY,
            "TABLES:\n" +
            "  salary(PK:salary_id, FK:emp_id, salary_year, salary_month, base_salary, meal_allowance, transport_allowance, position_allowance, overtime_pay, other_allowance, gross_salary, national_pension, health_insurance, long_term_care, employment_insurance, income_tax, local_income_tax, total_deduction, net_salary, pay_date, status ENUM('대기','완료'))\n" +
            "  deduction_rate(PK:rate_id, target_year, national_pension_rate, health_insurance_rate, long_term_care_rate, employment_insurance_rate)\n\n" +

            "RULES:\n" +
            "  v_salary_summary에서 지급상태 컬럼명은 salary_status (status 아님!)\n" +
            "  월급/실수령 → net_salary\n" +
            "  세전/지급합계 → gross_salary\n" +
            "  지급완료 → salary_status='완료' (뷰) 또는 status='완료' (raw 테이블)\n" +
            "  미지급/대기 → salary_status='대기' (뷰) 또는 status='대기' (raw 테이블)\n" +
            "  이번달 → salary_year=YEAR(CURDATE()) AND salary_month=MONTH(CURDATE())\n\n" +

            "EXAMPLES:\n" +
            "Q: 인사팀 이번달 급여 현황\n" +
            "A: SELECT emp_name, gross_salary, total_deduction, net_salary, salary_status FROM v_salary_summary WHERE dept_name = '인사팀' AND salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE());\n\n" +
            "Q: 미지급 급여 목록\n" +
            "A: SELECT emp_name, dept_name, salary_year, salary_month, net_salary, salary_status FROM v_salary_summary WHERE salary_status = '대기';\n\n" +
            "Q: 직급별 평균연봉\n" +
            "A: SELECT position_name, COUNT(*) AS cnt, ROUND(AVG(base_salary)) AS avg_salary FROM v_employee_full WHERE status = '재직' GROUP BY position_name, position_level ORDER BY position_level;\n\n"
        );

        // ──────────────────────────────────────────
        // ATTENDANCE
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.ATTENDANCE,
            "TABLES:\n" +
            "  attendance(PK:att_id, FK:emp_id, work_date, check_in, check_out, work_hours, overtime_hours, status ENUM('출근','지각','결근','휴가','출장'), note)\n" +
            "  overtime_request(PK:ot_id, FK:emp_id, ot_date, start_time, end_time, ot_hours, reason, status ENUM('대기','승인','반려'), FK:approver_id)\n\n" +

            "RULES:\n" +
            "  attendance/overtime_request에 뷰 없음 → raw 테이블 사용, employee JOIN 필요\n" +
            "  이번달 지각 → status='지각' AND YEAR(work_date)=YEAR(CURDATE()) AND MONTH(work_date)=MONTH(CURDATE())\n\n" +

            "EXAMPLES:\n" +
            "Q: 이번달 지각한 사람\n" +
            "A: SELECT e.emp_name, a.work_date, a.check_in FROM attendance a JOIN employee e ON a.emp_id = e.emp_id WHERE a.status = '지각' AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE());\n\n" +
            "Q: 이번달 초과근무 승인 건수와 총 시간\n" +
            "A: SELECT COUNT(*) AS approved_count, SUM(ot_hours) AS total_ot_hours FROM overtime_request WHERE status = '승인' AND YEAR(ot_date) = YEAR(CURDATE()) AND MONTH(ot_date) = MONTH(CURDATE());\n\n"
        );

        // ──────────────────────────────────────────
        // LEAVE
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.LEAVE,
            "TABLES:\n" +
            "  leave_request(PK:leave_id, FK:emp_id, leave_type ENUM('연차','반차','병가','경조사','공가'), half_type ENUM('오전','오후'), start_date, end_date, days, reason, status ENUM('대기','승인','반려','취소'), FK:approver_id)\n" +
            "  annual_leave(PK:al_id, FK:emp_id, leave_year, total_days, used_days, remain_days)\n\n" +

            "RULES:\n" +
            "  v_leave_status에서 휴가상태 컬럼명은 leave_status (status 아님!)\n" +
            "  승인된 휴가 → leave_status='승인' (뷰) 또는 status='승인' (raw 테이블)\n" +
            "  잔여 연차 → remain_days in v_leave_status\n\n" +

            "EXAMPLES:\n" +
            "Q: 연차 남은 일수가 3일 이하인 직원\n" +
            "A: SELECT emp_name, dept_name, remain_days FROM v_leave_status WHERE leave_year = YEAR(CURDATE()) AND remain_days <= 3 GROUP BY emp_id, emp_name, dept_name, remain_days;\n\n" +
            "Q: 대기중인 휴가 신청\n" +
            "A: SELECT emp_name, dept_name, leave_type, start_date, end_date, days FROM v_leave_status WHERE leave_status = '대기';\n\n"
        );

        // ──────────────────────────────────────────
        // EVALUATION
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.EVALUATION,
            "TABLES:\n" +
            "  evaluation(PK:eval_id, FK:emp_id, eval_year, eval_period ENUM('상반기','하반기','연간'), eval_type ENUM('자기평가','상위평가','동료평가'), total_score, grade ENUM('S','A','B','C','D'), eval_comment, eval_status ENUM('작성중','최종확정'), FK:evaluator_id)\n" +
            "  evaluation_item(PK:item_id, FK:eval_id, item_name, score, max_score)\n\n" +

            "RULES:\n" +
            "  evaluator_name은 v_evaluation_result에만 있음 (raw테이블에는 evaluator_id만)\n" +
            "  확정된 평가 → eval_status='최종확정'\n\n" +

            "EXAMPLES:\n" +
            "Q: 2024년 하반기 S등급 받은 직원\n" +
            "A: SELECT emp_name, dept_name, total_score, grade FROM v_evaluation_result WHERE eval_year = 2024 AND eval_period = '하반기' AND grade = 'S';\n\n" +
            "Q: 평가자별 평가 건수\n" +
            "A: SELECT evaluator_name, COUNT(*) AS eval_count FROM v_evaluation_result WHERE eval_status = '최종확정' GROUP BY evaluator_name ORDER BY eval_count DESC;\n\n"
        );

        // ──────────────────────────────────────────
        // ACCOUNT (★ 관리자 목록 오류 수정 핵심)
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.ACCOUNT,
            "TABLES:\n" +
            "  account(PK:account_id, FK:emp_id, username, role ENUM('관리자','HR담당자','일반'), is_active(1/0), login_attempts, last_login, password_changed_at, locked_at)\n\n" +

            "RULES:\n" +
            "  role 컬럼은 ONLY account 테이블에 있음. v_employee_full에는 role 없음!\n" +
            "  관리자/HR담당자/일반 → account.role 사용, 반드시 account JOIN employee 필요\n" +
            "  잠긴계정 → account.locked_at IS NOT NULL\n" +
            "  활성계정 → account.is_active=1 AND account.locked_at IS NULL\n\n" +

            "EXAMPLES:\n" +
            "Q: 관리자 목록\n" +
            "A: SELECT e.emp_name, e.email, e.phone, a.username, a.role FROM account a JOIN employee e ON a.emp_id = e.emp_id WHERE a.role = '관리자' AND a.is_active = 1;\n\n" +
            "Q: HR담당자 목록\n" +
            "A: SELECT e.emp_name, e.email, a.username, a.role FROM account a JOIN employee e ON a.emp_id = e.emp_id WHERE a.role = 'HR담당자' AND a.is_active = 1;\n\n" +
            "Q: 잠긴 계정 목록\n" +
            "A: SELECT a.account_id, e.emp_name, a.username, a.login_attempts, a.locked_at FROM account a JOIN employee e ON a.emp_id = e.emp_id WHERE a.locked_at IS NOT NULL;\n\n" +
            "Q: 권한별 계정 수\n" +
            "A: SELECT role, COUNT(*) AS cnt FROM account WHERE is_active = 1 GROUP BY role;\n\n"
        );

        // ──────────────────────────────────────────
        // SYSTEM
        // ──────────────────────────────────────────
        CATEGORY_PROMPTS.put(Category.SYSTEM,
            "TABLES:\n" +
            "  notification(PK:noti_id, FK:emp_id, noti_type, ref_table, ref_id, message, is_read(1/0), read_at)\n" +
            "  audit_log(PK:log_id, FK:actor_id→employee, target_table, target_id, action ENUM('INSERT','UPDATE','DELETE'), column_name, old_value, new_value, created_at)\n" +
            "  public_holiday(PK:holiday_id, holiday_date, holiday_name, holiday_year)\n\n" +

            "RULES:\n" +
            "  안읽은알림 → notification.is_read=0\n" +
            "  감사로그 actor → audit_log JOIN employee ON actor_id=emp_id (LEFT JOIN, NULL=시스템)\n\n" +

            "EXAMPLES:\n" +
            "Q: 2025년 공휴일 목록\n" +
            "A: SELECT holiday_date, holiday_name FROM public_holiday WHERE holiday_year = 2025 ORDER BY holiday_date;\n\n" +
            "Q: 최근 감사로그 10건\n" +
            "A: SELECT al.created_at, e.emp_name AS actor, al.target_table, al.action, al.column_name, al.old_value, al.new_value FROM audit_log al LEFT JOIN employee e ON al.actor_id = e.emp_id ORDER BY al.created_at DESC LIMIT 10;\n\n" +
            "Q: 안읽은 알림이 많은 직원\n" +
            "A: SELECT e.emp_name, COUNT(*) AS unread_count FROM notification n JOIN employee e ON n.emp_id = e.emp_id WHERE n.is_read = 0 GROUP BY n.emp_id, e.emp_name ORDER BY unread_count DESC;\n\n"
        );
    }

    // ================================================================
    // 5. 메인 빌드 메서드
    // ================================================================
    public static String build(String question) {
        Set<Category> matched = analyzeKeywords(question);

        if (matched.isEmpty()) {
            matched.add(Category.EMPLOYEE);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(BASE_PROMPT);

        for (Category cat : matched) {
            String catPrompt = CATEGORY_PROMPTS.get(cat);
            if (catPrompt != null) {
                sb.append("--- ").append(cat.name()).append(" ---\n");
                sb.append(catPrompt);
            }
        }

        sb.append("Question: ").append(question);
        return sb.toString();
    }

    // ================================================================
    // 6. 키워드 분석기
    // ================================================================
    private static Set<Category> analyzeKeywords(String question) {
        Set<Category> result = EnumSet.noneOf(Category.class);

        for (Map.Entry<Category, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (question.contains(keyword)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }

        return result;
    }

    // ================================================================
    // 7. 디버깅용
    // ================================================================
    public static String getMatchedCategories(String question) {
        Set<Category> matched = analyzeKeywords(question);
        if (matched.isEmpty()) {
            return "[DEFAULT: EMPLOYEE]";
        }
        return matched.toString();
    }

    public static int estimateTokens(String prompt) {
        return (int) (prompt.length() / 2.5);
    }
}