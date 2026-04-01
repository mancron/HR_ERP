package com.hrms.sys.util;

import java.util.*;

/**
 * Dynamic Prompt Builder v3 — Optimized for small (3B) LLM models
 *
 * Improvements over v2:
 *   [1] Language unification : All instructions/rules in English.
 *                              Korean appears ONLY for actual DB stored values.
 *   [2] Lockdown fallback    : Returns the single token "불가능" when the schema
 *                              cannot answer — prevents hallucinated SQL.
 *   [3] Standard DDL         : CREATE TABLE syntax with real data types from hr_erp_schema.sql.
 *                              Comments (--) replace the ad-hoc "PK:col" notation.
 *   [4] FK relation map      : Cross-domain foreign key map fixed in the header,
 *                              solving multi-table JOIN failures.
 *   [5] Weighted keyword scoring → single best-match category selected,
 *                              eliminating multi-category prompt bloat.
 *   [6] Dynamic few-shot     : Tag-based similarity scoring; injects top 1–2
 *                              most-relevant examples only (no more hardcoded 10+).
 *
 * Usage:
 *   String prompt = DynamicPromptBuilder.build("관리자 목록");
 *
 * Debug:
 *   DynamicPromptBuilder.getMatchedCategory("관리자 목록"); // "ACCOUNT"
 *   DynamicPromptBuilder.estimateTokens(prompt);           // ~int
 */
public class DynamicPromptBuilder {

    // ================================================================
    // 1. Category enum
    // ================================================================
    private enum Category {
        EMPLOYEE, SALARY, ATTENDANCE, LEAVE, EVALUATION, ACCOUNT, SYSTEM
    }

    // ================================================================
    // 2. Weighted keyword map
    //    Weight 3 = highly domain-specific  (강한 신호)
    //    Weight 2 = moderately specific     (중간 신호)
    //    Weight 1 = general / shared        (약한 신호)
    //
    //    Design intent: Weight 3 keywords for ACCOUNT/EVALUATION/LEAVE
    //    are intentionally higher than EMPLOYEE general terms so that
    //    "관리자 목록" correctly routes to ACCOUNT (not EMPLOYEE).
    // ================================================================
    private static final Map<Category, Map<String, Integer>> WEIGHTED_KEYWORDS = new LinkedHashMap<>();

    static {
        /* ── EMPLOYEE ─────────────────────────────── */
        Map<String, Integer> emp = new LinkedHashMap<>();
        // Position names — weight 3 so "부장" beats generic "직원"
        for (String k : new String[]{"사원","대리","과장","차장","부장","직급","직급별"}) emp.put(k, 3);
        // Employment status
        for (String k : new String[]{"재직","퇴직","휴직","퇴사","복직"}) emp.put(k, 3);
        // Employment type
        for (String k : new String[]{"정규직","계약직","파트타임"}) emp.put(k, 3);
        // Personnel action
        for (String k : new String[]{"발령","승진","전보","인사이동"}) emp.put(k, 3);
        // Demographics
        for (String k : new String[]{"성별","생년","출생","나이","만나이","근속","입사"}) emp.put(k, 2);
        // General
        for (String k : new String[]{"직원","인원","명단","부서","부서별","연봉","기본급","이름","검색"}) emp.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.EMPLOYEE, emp);

        /* ── SALARY ───────────────────────────────── */
        Map<String, Integer> sal = new LinkedHashMap<>();
        for (String k : new String[]{"급여","월급","실수령","세전","세후","공제","수당","명세","미지급","지급완료","급여대기"}) sal.put(k, 3);
        for (String k : new String[]{"국민연금","건강보험","고용보험","소득세","식대","교통비","직책수당","장기요양"}) sal.put(k, 2);
        for (String k : new String[]{"지급","공제율","보험료"}) sal.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.SALARY, sal);

        /* ── ATTENDANCE ───────────────────────────── */
        Map<String, Integer> att = new LinkedHashMap<>();
        for (String k : new String[]{"출근","퇴근","지각","결근","근태","출퇴근","초과근무","야근","잔업","오버타임"}) att.put(k, 3);
        for (String k : new String[]{"근무시간","출장"}) att.put(k, 2);
        for (String k : new String[]{"근무"}) att.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.ATTENDANCE, att);

        /* ── LEAVE ────────────────────────────────── */
        Map<String, Integer> lev = new LinkedHashMap<>();
        for (String k : new String[]{"연차","반차","병가","경조사","공가","잔여연차","휴가일수","남은연차"}) lev.put(k, 3);
        for (String k : new String[]{"휴가신청","휴가승인","휴가반려"}) lev.put(k, 2);
        for (String k : new String[]{"휴가","잔여","사용"}) lev.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.LEAVE, lev);

        /* ── EVALUATION ───────────────────────────── */
        Map<String, Integer> eva = new LinkedHashMap<>();
        for (String k : new String[]{"평가","인사평가","고과","등급","S등급","A등급","B등급","C등급","D등급"}) eva.put(k, 3);
        for (String k : new String[]{"상반기평가","하반기평가","자기평가","상위평가","동료평가","최종확정"}) eva.put(k, 2);
        for (String k : new String[]{"점수","평가자"}) eva.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.EVALUATION, eva);

        /* ── ACCOUNT ──────────────────────────────── */
        Map<String, Integer> acc = new LinkedHashMap<>();
        // "관리자" weight 3 — must NOT route to EMPLOYEE (which has weight 1 only)
        for (String k : new String[]{"관리자","HR담당자","계정","잠금","잠긴","권한","username","아이디"}) acc.put(k, 3);
        for (String k : new String[]{"로그인","로그아웃","비밀번호","활성","비활성","차단","로그인실패"}) acc.put(k, 2);
        WEIGHTED_KEYWORDS.put(Category.ACCOUNT, acc);

        /* ── SYSTEM ───────────────────────────────── */
        Map<String, Integer> sys = new LinkedHashMap<>();
        for (String k : new String[]{"감사로그","변경이력","감사","알림","공휴일"}) sys.put(k, 3);
        for (String k : new String[]{"로그","이력","안읽은","읽은","휴일","명절"}) sys.put(k, 2);
        WEIGHTED_KEYWORDS.put(Category.SYSTEM, sys);
    }

    // ================================================================
    // 3. Fixed HEADER — always prepended to every prompt
    //
    //    Layout (attention priority order for small models):
    //      A. Task definition + output format
    //      B. FALLBACK RULE  ← highest priority, placed early
    //      C. Critical column constraints
    //      D. Cross-domain FK relation map
    //      E. Available views index
    // ================================================================
    private static final String HEADER =
        "You are a MySQL 8.0 expert for an HR-ERP system.\n" +
        "Output ONLY a raw MySQL SELECT query ending with a semicolon.\n" +
        "No markdown fences, no explanation, no preamble.\n" +
        "Use ONLY columns defined in the DDL below. NEVER invent columns.\n" +
        "Default LIMIT: 100 (omit if aggregation only).\n\n" +

        // ── B. Lockdown ──────────────────────────────────────────────
        "## FALLBACK RULE  ← HIGHEST PRIORITY\n" +
        "If the question CANNOT be answered with the given schema,\n" +
        "output EXACTLY the single word:  불가능\n" +
        "Do NOT generate any SQL in that case.\n\n" +

        // ── C. Critical column constraints ───────────────────────────
        "## CRITICAL COLUMN CONSTRAINTS\n" +
        "- `role`            : EXISTS ONLY in `account`. Views have NO `role` column.\n" +
        "- `position_name`   : Use for 부장/차장/과장/대리/사원 filtering. NEVER use `emp_type` for these.\n" +
        "- `emp_type`        : ONLY for '정규직'/'계약직'/'파트타임'. NEVER for position titles.\n" +
        "- `manager_id`      : EXISTS ONLY in `department`. Views have NO `manager_id`.\n" +
        "- `age`             : NO such column. Use TIMESTAMPDIFF(YEAR, birth_date, CURDATE()).\n" +
        "- `gender`          : CHAR(1) values are 'M' (남) and 'F' (여). NEVER '남자','여자','남','여'.\n" +
        "- `v_employee_full` : does NOT contain dept_id, role, manager_id, locked_at.\n" +
        "- `v_salary_summary`: payment state column is `salary_status`  (NOT `status`).\n" +
        "- `v_leave_status`  : leave state column   is `leave_status`   (NOT `status`).\n" +
        "- `evaluator_name`  : EXISTS ONLY in `v_evaluation_result`. Raw `evaluation` has only `evaluator_id`.\n\n" +

        // ── D. FK Relation Map ───────────────────────────────────────
        "## FK RELATION MAP\n" +
        "employee.dept_id          → department.dept_id\n" +
        "employee.position_id      → job_position.position_id\n" +
        "department.manager_id     → employee.emp_id\n" +
        "account.emp_id            → employee.emp_id\n" +
        "salary.emp_id             → employee.emp_id\n" +
        "attendance.emp_id         → employee.emp_id\n" +
        "overtime_request.emp_id   → employee.emp_id\n" +
        "overtime_request.approver_id → employee.emp_id\n" +
        "leave_request.emp_id      → employee.emp_id\n" +
        "leave_request.approver_id → employee.emp_id\n" +
        "annual_leave.emp_id       → employee.emp_id\n" +
        "evaluation.emp_id         → employee.emp_id\n" +
        "evaluation.evaluator_id   → employee.emp_id\n" +
        "personnel_history.emp_id  → employee.emp_id\n" +
        "personnel_history.approved_by → employee.emp_id\n" +
        "audit_log.actor_id        → employee.emp_id  (LEFT JOIN; NULL = system action)\n" +
        "notification.emp_id       → employee.emp_id\n\n" +

        // ── E. Views index ───────────────────────────────────────────
        "## VIEWS  (prefer when all needed columns exist)\n" +
        "v_employee_full    : emp_id, emp_name, emp_no, status, emp_type, base_salary, hire_date, resign_date, birth_date, gender, email, phone, dept_name, position_name, position_level\n" +
        "v_salary_summary   : emp_id, emp_name, emp_no, dept_name, position_name, salary_year, salary_month, base_salary, gross_salary, total_deduction, net_salary, overtime_pay, salary_status, pay_date\n" +
        "v_leave_status     : emp_id, emp_name, emp_no, dept_name, position_name, leave_id, leave_type, half_type, start_date, end_date, days, reason, leave_status, approved_at, reject_reason, leave_year, total_days, used_days, remain_days\n" +
        "v_evaluation_result: emp_id, emp_name, emp_no, dept_name, position_name, eval_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, confirmed_at, evaluator_name\n\n";

    // ================================================================
    // 4. Standard DDL per category
    //    - Data types mirror hr_erp_schema.sql v4
    //    - Only columns needed for SELECT queries are listed
    //      (password_hash, addresses, bank_account omitted — never queried)
    // ================================================================
    private static final Map<Category, String> DDL = new LinkedHashMap<>();

    static {

        /* ─────────────────── EMPLOYEE ──────────────────────────────── */
        DDL.put(Category.EMPLOYEE,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE job_position (\n" +
            "  position_id    INT          PRIMARY KEY,\n" +
            "  position_name  VARCHAR(20)  NOT NULL,   -- 사원/대리/과장/차장/부장\n" +
            "  position_level INT          NOT NULL,   -- 1=사원 ~ 5=부장\n" +
            "  base_salary    INT          NOT NULL,\n" +
            "  is_active      TINYINT(1)   NOT NULL\n" +
            ");\n\n" +

            "CREATE TABLE department (\n" +
            "  dept_id        INT          PRIMARY KEY,\n" +
            "  dept_name      VARCHAR(50)  NOT NULL,\n" +
            "  parent_dept_id INT,                     -- self-referencing FK\n" +
            "  manager_id     INT,                     -- FK → employee.emp_id\n" +
            "  dept_level     INT          NOT NULL,\n" +
            "  is_active      TINYINT(1)   NOT NULL\n" +
            ");\n\n" +

            "CREATE TABLE employee (\n" +
            "  emp_id      INT          PRIMARY KEY,\n" +
            "  emp_name    VARCHAR(20)  NOT NULL,\n" +
            "  emp_no      VARCHAR(20)  NOT NULL UNIQUE,\n" +
            "  dept_id     INT          NOT NULL,       -- FK → department.dept_id\n" +
            "  position_id INT          NOT NULL,       -- FK → job_position.position_id\n" +
            "  hire_date   DATE         NOT NULL,\n" +
            "  resign_date DATE,                        -- NULL = currently employed\n" +
            "  emp_type    VARCHAR(10)  NOT NULL,       -- '정규직' | '계약직' | '파트타임'\n" +
            "  status      VARCHAR(10)  NOT NULL,       -- '재직' | '휴직' | '퇴직'\n" +
            "  base_salary INT          NOT NULL,\n" +
            "  birth_date  DATE,\n" +
            "  gender      CHAR(1),                     -- 'M' | 'F'\n" +
            "  email       VARCHAR(100),\n" +
            "  phone       VARCHAR(20)\n" +
            ");\n\n" +

            "CREATE TABLE personnel_history (\n" +
            "  history_id       INT         PRIMARY KEY,\n" +
            "  emp_id           INT         NOT NULL,   -- FK → employee.emp_id\n" +
            "  change_type      VARCHAR(20) NOT NULL,   -- '발령'|'승진'|'전보'|'퇴직'|'복직'\n" +
            "  from_dept_id     INT,\n" +
            "  to_dept_id       INT,\n" +
            "  from_position_id INT,\n" +
            "  to_position_id   INT,\n" +
            "  change_date      DATE        NOT NULL,\n" +
            "  reason           VARCHAR(200),\n" +
            "  approved_by      INT                     -- FK → employee.emp_id\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- Position title filter : WHERE position_name = '부장'  → use v_employee_full\n" +
            "- Emp type filter       : WHERE emp_type = '정규직'\n" +
            "- Status filter         : WHERE status = '재직'\n" +
            "- Age calculation       : TIMESTAMPDIFF(YEAR, birth_date, CURDATE())\n" +
            "- Tenure years          : TIMESTAMPDIFF(YEAR, hire_date, CURDATE())\n" +
            "- Gender filter         : gender = 'M'  (male) | gender = 'F'  (female)\n" +
            "- Dept head query       : FROM department d JOIN employee e ON d.manager_id = e.emp_id\n" +
            "- Group by dept         : GROUP BY dept_name  (use v_employee_full)\n\n"
        );

        /* ─────────────────── SALARY ─────────────────────────────────── */
        DDL.put(Category.SALARY,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE salary (\n" +
            "  salary_id            INT         PRIMARY KEY,\n" +
            "  emp_id               INT         NOT NULL,   -- FK → employee.emp_id\n" +
            "  salary_year          INT         NOT NULL,\n" +
            "  salary_month         INT         NOT NULL,   -- 1 ~ 12\n" +
            "  base_salary          INT         NOT NULL DEFAULT 0,\n" +
            "  meal_allowance       INT         NOT NULL DEFAULT 0,\n" +
            "  transport_allowance  INT         NOT NULL DEFAULT 0,\n" +
            "  position_allowance   INT         NOT NULL DEFAULT 0,\n" +
            "  overtime_pay         INT         NOT NULL DEFAULT 0,\n" +
            "  other_allowance      INT         NOT NULL DEFAULT 0,\n" +
            "  gross_salary         INT         NOT NULL DEFAULT 0,  -- sum of all allowances\n" +
            "  national_pension     INT         NOT NULL DEFAULT 0,\n" +
            "  health_insurance     INT         NOT NULL DEFAULT 0,\n" +
            "  long_term_care       INT         NOT NULL DEFAULT 0,\n" +
            "  employment_insurance INT         NOT NULL DEFAULT 0,\n" +
            "  income_tax           INT         NOT NULL DEFAULT 0,\n" +
            "  local_income_tax     INT         NOT NULL DEFAULT 0,\n" +
            "  total_deduction      INT         NOT NULL DEFAULT 0,  -- sum of all deductions\n" +
            "  net_salary           INT         NOT NULL DEFAULT 0,  -- gross - total_deduction\n" +
            "  pay_date             DATE,\n" +
            "  status               VARCHAR(10) NOT NULL DEFAULT '대기'  -- '대기' | '완료'\n" +
            ");\n\n" +

            "CREATE TABLE deduction_rate (\n" +
            "  rate_id                   INT          PRIMARY KEY,\n" +
            "  target_year               INT          NOT NULL UNIQUE,\n" +
            "  national_pension_rate     DECIMAL(6,5) NOT NULL,  -- e.g. 0.04500\n" +
            "  health_insurance_rate     DECIMAL(6,5) NOT NULL,\n" +
            "  long_term_care_rate       DECIMAL(6,5) NOT NULL,\n" +
            "  employment_insurance_rate DECIMAL(6,5) NOT NULL\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- View `v_salary_summary` : use `salary_status` (NOT `status`) for payment state.\n" +
            "- Raw table `salary`      : use `status` for payment state.\n" +
            "- Net pay    : net_salary\n" +
            "- Gross pay  : gross_salary\n" +
            "- Paid       : salary_status = '완료'  (view) | status = '완료'  (raw)\n" +
            "- Pending    : salary_status = '대기'  (view) | status = '대기'  (raw)\n" +
            "- This month : salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE())\n\n"
        );

        /* ─────────────────── ATTENDANCE ─────────────────────────────── */
        DDL.put(Category.ATTENDANCE,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE attendance (\n" +
            "  att_id         INT          PRIMARY KEY,\n" +
            "  emp_id         INT          NOT NULL,  -- FK → employee.emp_id\n" +
            "  work_date      DATE         NOT NULL,\n" +
            "  check_in       TIME,\n" +
            "  check_out      TIME,\n" +
            "  work_hours     DECIMAL(4,2),\n" +
            "  overtime_hours DECIMAL(4,2) NOT NULL DEFAULT 0,\n" +
            "  status         VARCHAR(20)  NOT NULL   -- '출근'|'지각'|'결근'|'휴가'|'출장'\n" +
            ");\n\n" +

            "CREATE TABLE overtime_request (\n" +
            "  ot_id       INT          PRIMARY KEY,\n" +
            "  emp_id      INT          NOT NULL,   -- FK → employee.emp_id\n" +
            "  ot_date     DATE         NOT NULL,\n" +
            "  start_time  TIME         NOT NULL,\n" +
            "  end_time    TIME         NOT NULL,\n" +
            "  ot_hours    DECIMAL(4,2) NOT NULL,\n" +
            "  reason      VARCHAR(300),\n" +
            "  status      VARCHAR(10)  NOT NULL,   -- '대기'|'승인'|'반려'\n" +
            "  approver_id INT,                     -- FK → employee.emp_id\n" +
            "  approved_at DATETIME\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- No views for attendance/overtime. Always JOIN employee ON emp_id.\n" +
            "- Late filter         : status = '지각'\n" +
            "- Absent filter       : status = '결근'\n" +
            "- Overtime approved   : overtime_request.status = '승인'\n" +
            "- This month          : YEAR(work_date) = YEAR(CURDATE()) AND MONTH(work_date) = MONTH(CURDATE())\n\n"
        );

        /* ─────────────────── LEAVE ──────────────────────────────────── */
        DDL.put(Category.LEAVE,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE leave_request (\n" +
            "  leave_id     INT          PRIMARY KEY,\n" +
            "  emp_id       INT          NOT NULL,   -- FK → employee.emp_id\n" +
            "  leave_type   VARCHAR(20)  NOT NULL,   -- '연차'|'반차'|'병가'|'경조사'|'공가'\n" +
            "  half_type    VARCHAR(10),             -- '오전'|'오후'  (반차 only)\n" +
            "  start_date   DATE         NOT NULL,\n" +
            "  end_date     DATE         NOT NULL,\n" +
            "  days         DECIMAL(4,1) NOT NULL,\n" +
            "  reason       VARCHAR(500),\n" +
            "  status       VARCHAR(10)  NOT NULL,   -- '대기'|'승인'|'반려'|'취소'  (raw table)\n" +
            "  approver_id  INT,                     -- FK → employee.emp_id\n" +
            "  approved_at  DATETIME,\n" +
            "  reject_reason VARCHAR(200)\n" +
            ");\n\n" +

            "CREATE TABLE annual_leave (\n" +
            "  al_id       INT          PRIMARY KEY,\n" +
            "  emp_id      INT          NOT NULL,   -- FK → employee.emp_id\n" +
            "  leave_year  INT          NOT NULL,\n" +
            "  total_days  DECIMAL(4,1) NOT NULL,\n" +
            "  used_days   DECIMAL(4,1) NOT NULL,\n" +
            "  remain_days DECIMAL(4,1) NOT NULL\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- View `v_leave_status` : use `leave_status` (NOT `status`) for leave state.\n" +
            "- Raw table             : use `status`.\n" +
            "- Approved filter       : leave_status = '승인'  (view) | status = '승인'  (raw)\n" +
            "- Pending filter        : leave_status = '대기'  (view) | status = '대기'  (raw)\n" +
            "- Remaining days        : remain_days  in v_leave_status or annual_leave\n\n"
        );

        /* ─────────────────── EVALUATION ─────────────────────────────── */
        DDL.put(Category.EVALUATION,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE evaluation (\n" +
            "  eval_id      INT          PRIMARY KEY,\n" +
            "  emp_id       INT          NOT NULL,   -- FK → employee.emp_id  (evaluatee)\n" +
            "  eval_year    INT          NOT NULL,\n" +
            "  eval_period  VARCHAR(10)  NOT NULL,   -- '상반기'|'하반기'|'연간'\n" +
            "  eval_type    VARCHAR(20)  NOT NULL,   -- '자기평가'|'상위평가'|'동료평가'\n" +
            "  total_score  DECIMAL(5,2),            -- 0 ~ 100\n" +
            "  grade        VARCHAR(5),              -- 'S'|'A'|'B'|'C'|'D'\n" +
            "  eval_comment TEXT,\n" +
            "  eval_status  VARCHAR(10)  NOT NULL,   -- '작성중'|'최종확정'\n" +
            "  evaluator_id INT,                     -- FK → employee.emp_id  (evaluator)\n" +
            "  confirmed_at DATETIME\n" +
            ");\n\n" +

            "CREATE TABLE evaluation_item (\n" +
            "  item_id   INT          PRIMARY KEY,\n" +
            "  eval_id   INT          NOT NULL,      -- FK → evaluation.eval_id\n" +
            "  item_name VARCHAR(50)  NOT NULL,      -- e.g. 업무성과/직무역량/리더십\n" +
            "  score     DECIMAL(5,2) NOT NULL,\n" +
            "  max_score DECIMAL(5,2) NOT NULL\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- `evaluator_name` exists ONLY in `v_evaluation_result`. Raw table has only `evaluator_id`.\n" +
            "- Confirmed filter : eval_status = '최종확정'\n" +
            "- Grade filter     : grade = 'S'  (or A/B/C/D)\n\n"
        );

        /* ─────────────────── ACCOUNT ────────────────────────────────── */
        DDL.put(Category.ACCOUNT,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE account (\n" +
            "  account_id          INT          PRIMARY KEY,\n" +
            "  emp_id              INT          NOT NULL UNIQUE,  -- FK → employee.emp_id\n" +
            "  username            VARCHAR(50)  NOT NULL UNIQUE,\n" +
            "  role                VARCHAR(20)  NOT NULL,         -- '관리자'|'HR담당자'|'일반'\n" +
            "  is_active           TINYINT(1)   NOT NULL,\n" +
            "  login_attempts      INT          NOT NULL DEFAULT 0,\n" +
            "  last_login          DATETIME,\n" +
            "  password_changed_at DATETIME,\n" +
            "  locked_at           DATETIME                       -- NULL = not locked\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- `role` EXISTS ONLY in `account`. Views do NOT have `role`.\n" +
            "- Role filter     : role = '관리자' | 'HR담당자' | '일반'\n" +
            "- Locked account  : locked_at IS NOT NULL\n" +
            "- Active account  : is_active = 1 AND locked_at IS NULL\n" +
            "- Always JOIN employee to retrieve emp_name, email, phone.\n\n"
        );

        /* ─────────────────── SYSTEM ─────────────────────────────────── */
        DDL.put(Category.SYSTEM,
            "## DDL\n" +
            "```sql\n" +
            "CREATE TABLE notification (\n" +
            "  noti_id   BIGINT       PRIMARY KEY,\n" +
            "  emp_id    INT          NOT NULL,    -- FK → employee.emp_id\n" +
            "  noti_type VARCHAR(30)  NOT NULL,\n" +
            "  ref_table VARCHAR(50),\n" +
            "  ref_id    INT,\n" +
            "  message   VARCHAR(300) NOT NULL,\n" +
            "  is_read   TINYINT(1)   NOT NULL DEFAULT 0,  -- 0=unread, 1=read\n" +
            "  read_at   DATETIME,\n" +
            "  created_at DATETIME   NOT NULL\n" +
            ");\n\n" +

            "CREATE TABLE audit_log (\n" +
            "  log_id       BIGINT      PRIMARY KEY,\n" +
            "  actor_id     INT,                    -- FK → employee.emp_id (NULL = system)\n" +
            "  target_table VARCHAR(50) NOT NULL,\n" +
            "  target_id    INT         NOT NULL,\n" +
            "  action       VARCHAR(10) NOT NULL,   -- 'INSERT'|'UPDATE'|'DELETE'\n" +
            "  column_name  VARCHAR(50),\n" +
            "  old_value    TEXT,\n" +
            "  new_value    TEXT,\n" +
            "  created_at   DATETIME    NOT NULL\n" +
            ");\n\n" +

            "CREATE TABLE public_holiday (\n" +
            "  holiday_id   INT         PRIMARY KEY,\n" +
            "  holiday_date DATE        NOT NULL,\n" +
            "  holiday_name VARCHAR(50) NOT NULL,\n" +
            "  holiday_year INT         NOT NULL\n" +
            ");\n" +
            "```\n\n" +

            "## RULES\n" +
            "- Unread notifications : is_read = 0\n" +
            "- Audit log actor      : LEFT JOIN employee ON actor_id = emp_id (NULL = system)\n" +
            "- audit_log tracks     : employee(base_salary/status/resign_date), account(role/is_active), annual_leave(used_days/remain_days), salary(status)\n\n"
        );
    }

    // ================================================================
    // 5. Example pool
    //    Each Example carries:
    //      - category : routing key
    //      - tags     : keywords for similarity scoring (vs user question)
    //      - qa       : "Q: ...\nA: ...;\n"
    //
    //    Design: 3–4 examples per category; quality over quantity.
    //    The pickTopExamples() method selects only 1–2 at runtime.
    // ================================================================
    private static class Example {
        final Category category;
        final String[] tags;
        final String qa;

        Example(Category cat, String[] tags, String qa) {
            this.category = cat;
            this.tags = tags;
            this.qa = qa;
        }
    }

    private static final List<Example> EXAMPLE_POOL = new ArrayList<>();

    static {
        /* ── EMPLOYEE ──────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부장","직급","과장","차장"},
            "Q: 부장 목록\n" +
            "A: SELECT emp_name, dept_name, position_name, email, phone\n" +
            "   FROM v_employee_full\n" +
            "   WHERE position_name = '부장' AND status = '재직';\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부서장","팀장","매니저"},
            "Q: 부서장 목록\n" +
            "A: SELECT e.emp_name, d.dept_name, p.position_name\n" +
            "   FROM department d\n" +
            "   JOIN employee e ON d.manager_id = e.emp_id\n" +
            "   JOIN job_position p ON e.position_id = p.position_id\n" +
            "   WHERE d.is_active = 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"나이","연령","만나이","세","30","40"},
            "Q: 30세 이상 40세 이하 재직 직원\n" +
            "A: SELECT emp_name, dept_name,\n" +
            "          TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS age\n" +
            "   FROM v_employee_full\n" +
            "   WHERE status = '재직'\n" +
            "     AND TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) BETWEEN 30 AND 40;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"발령","승진","전보","인사이동"},
            "Q: 올해 발령 이력\n" +
            "A: SELECT e.emp_name, ph.change_type, ph.change_date, ph.reason\n" +
            "   FROM personnel_history ph\n" +
            "   JOIN employee e ON ph.emp_id = e.emp_id\n" +
            "   WHERE YEAR(ph.change_date) = YEAR(CURDATE())\n" +
            "   ORDER BY ph.change_date DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부서","직원수","인원수","통계","부서별"},
            "Q: 부서별 재직 직원 수\n" +
            "A: SELECT dept_name, COUNT(*) AS emp_count\n" +
            "   FROM v_employee_full\n" +
            "   WHERE status = '재직'\n" +
            "   GROUP BY dept_name\n" +
            "   ORDER BY emp_count DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"근속","년","경력","5년"},
            "Q: 근속 5년 이상 직원\n" +
            "A: SELECT emp_name, dept_name, hire_date,\n" +
            "          TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) AS years_worked\n" +
            "   FROM v_employee_full\n" +
            "   WHERE status = '재직'\n" +
            "     AND TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) >= 5\n" +
            "   ORDER BY hire_date;\n"));

        /* ── SALARY ────────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"급여","이번달","현황","월급"},
            "Q: 이번달 급여 현황\n" +
            "A: SELECT emp_name, dept_name, gross_salary, total_deduction, net_salary, salary_status\n" +
            "   FROM v_salary_summary\n" +
            "   WHERE salary_year = YEAR(CURDATE())\n" +
            "     AND salary_month = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"미지급","대기","미완료","지급안된"},
            "Q: 미지급 급여 목록\n" +
            "A: SELECT emp_name, dept_name, salary_year, salary_month, net_salary, salary_status\n" +
            "   FROM v_salary_summary\n" +
            "   WHERE salary_status = '대기';\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"공제","국민연금","건강보험","고용보험","항목"},
            "Q: 이번달 공제 내역\n" +
            "A: SELECT emp_name, national_pension, health_insurance,\n" +
            "          employment_insurance, income_tax, total_deduction\n" +
            "   FROM v_salary_summary\n" +
            "   WHERE salary_year = YEAR(CURDATE())\n" +
            "     AND salary_month = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"직급","평균","연봉"},
            "Q: 직급별 평균 연봉\n" +
            "A: SELECT position_name, COUNT(*) AS cnt, ROUND(AVG(base_salary)) AS avg_salary\n" +
            "   FROM v_employee_full\n" +
            "   WHERE status = '재직'\n" +
            "   GROUP BY position_name, position_level\n" +
            "   ORDER BY position_level;\n"));

        /* ── ATTENDANCE ─────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"지각","이번달","이번주"},
            "Q: 이번달 지각 직원\n" +
            "A: SELECT e.emp_name, a.work_date, a.check_in\n" +
            "   FROM attendance a\n" +
            "   JOIN employee e ON a.emp_id = e.emp_id\n" +
            "   WHERE a.status = '지각'\n" +
            "     AND YEAR(a.work_date) = YEAR(CURDATE())\n" +
            "     AND MONTH(a.work_date) = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"초과근무","야근","잔업","승인","오버타임"},
            "Q: 이번달 승인된 초과근무 건수 및 총 시간\n" +
            "A: SELECT COUNT(*) AS approved_count, SUM(ot_hours) AS total_ot_hours\n" +
            "   FROM overtime_request\n" +
            "   WHERE status = '승인'\n" +
            "     AND YEAR(ot_date) = YEAR(CURDATE())\n" +
            "     AND MONTH(ot_date) = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"결근","이번달"},
            "Q: 이번달 결근 현황\n" +
            "A: SELECT e.emp_name, a.work_date\n" +
            "   FROM attendance a\n" +
            "   JOIN employee e ON a.emp_id = e.emp_id\n" +
            "   WHERE a.status = '결근'\n" +
            "     AND YEAR(a.work_date) = YEAR(CURDATE())\n" +
            "     AND MONTH(a.work_date) = MONTH(CURDATE());\n"));

        /* ── LEAVE ─────────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new String[]{"잔여","남은","연차","3일"},
            "Q: 잔여 연차 3일 이하 직원\n" +
            "A: SELECT emp_name, dept_name, remain_days\n" +
            "   FROM v_leave_status\n" +
            "   WHERE leave_year = YEAR(CURDATE()) AND remain_days <= 3\n" +
            "   GROUP BY emp_id, emp_name, dept_name, remain_days;\n"));

        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new String[]{"대기","신청","승인대기","휴가"},
            "Q: 승인 대기 중인 휴가 신청\n" +
            "A: SELECT emp_name, dept_name, leave_type, start_date, end_date, days\n" +
            "   FROM v_leave_status\n" +
            "   WHERE leave_status = '대기';\n"));

        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new String[]{"병가","경조사"},
            "Q: 올해 병가 사용 현황\n" +
            "A: SELECT emp_name, dept_name, start_date, end_date, days\n" +
            "   FROM v_leave_status\n" +
            "   WHERE leave_type = '병가'\n" +
            "     AND leave_year = YEAR(CURDATE())\n" +
            "     AND leave_status = '승인';\n"));

        /* ── EVALUATION ─────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"S등급","하반기","상반기","등급"},
            "Q: 2024년 하반기 S등급 직원\n" +
            "A: SELECT emp_name, dept_name, total_score, grade\n" +
            "   FROM v_evaluation_result\n" +
            "   WHERE eval_year = 2024\n" +
            "     AND eval_period = '하반기'\n" +
            "     AND grade = 'S';\n"));

        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"평가자","건수","담당"},
            "Q: 평가자별 평가 건수\n" +
            "A: SELECT evaluator_name, COUNT(*) AS eval_count\n" +
            "   FROM v_evaluation_result\n" +
            "   WHERE eval_status = '최종확정'\n" +
            "   GROUP BY evaluator_name\n" +
            "   ORDER BY eval_count DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"부서","평균","점수"},
            "Q: 부서별 평균 평가 점수\n" +
            "A: SELECT dept_name, ROUND(AVG(total_score), 2) AS avg_score\n" +
            "   FROM v_evaluation_result\n" +
            "   WHERE eval_status = '최종확정'\n" +
            "   GROUP BY dept_name\n" +
            "   ORDER BY avg_score DESC;\n"));

        /* ── ACCOUNT ────────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"관리자","목록"},
            "Q: 관리자 목록\n" +
            "A: SELECT e.emp_name, e.email, e.phone, a.username, a.role\n" +
            "   FROM account a\n" +
            "   JOIN employee e ON a.emp_id = e.emp_id\n" +
            "   WHERE a.role = '관리자' AND a.is_active = 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"잠긴","잠금","차단","계정","로그인실패"},
            "Q: 잠긴 계정 목록\n" +
            "A: SELECT a.account_id, e.emp_name, a.username, a.login_attempts, a.locked_at\n" +
            "   FROM account a\n" +
            "   JOIN employee e ON a.emp_id = e.emp_id\n" +
            "   WHERE a.locked_at IS NOT NULL;\n"));

        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"권한","역할","현황","HR담당자"},
            "Q: 권한별 계정 수\n" +
            "A: SELECT role, COUNT(*) AS cnt\n" +
            "   FROM account\n" +
            "   WHERE is_active = 1\n" +
            "   GROUP BY role;\n"));

        /* ── SYSTEM ─────────────────────────────────────── */
        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"공휴일","휴일","명절"},
            "Q: 올해 공휴일 목록\n" +
            "A: SELECT holiday_date, holiday_name\n" +
            "   FROM public_holiday\n" +
            "   WHERE holiday_year = YEAR(CURDATE())\n" +
            "   ORDER BY holiday_date;\n"));

        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"감사","로그","이력","변경"},
            "Q: 최근 감사로그 10건\n" +
            "A: SELECT al.created_at, e.emp_name AS actor, al.target_table,\n" +
            "          al.action, al.column_name, al.old_value, al.new_value\n" +
            "   FROM audit_log al\n" +
            "   LEFT JOIN employee e ON al.actor_id = e.emp_id\n" +
            "   ORDER BY al.created_at DESC\n" +
            "   LIMIT 10;\n"));

        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"알림","안읽은","읽지않은"},
            "Q: 안읽은 알림이 많은 직원\n" +
            "A: SELECT e.emp_name, COUNT(*) AS unread_count\n" +
            "   FROM notification n\n" +
            "   JOIN employee e ON n.emp_id = e.emp_id\n" +
            "   WHERE n.is_read = 0\n" +
            "   GROUP BY n.emp_id, e.emp_name\n" +
            "   ORDER BY unread_count DESC;\n"));
    }

    // ================================================================
    // 6. Public API
    // ================================================================

    /**
     * Build a fully assembled prompt for the given natural-language question.
     *
     * @param question  Korean or mixed natural-language question about HR data
     * @return          Complete prompt string ready to send to the LLM
     */
    public static String build(String question) {
        Category best     = pickBestCategory(question);
        List<Example> top = pickTopExamples(question, best, 2);

        StringBuilder sb = new StringBuilder(2048);
        sb.append(HEADER);
        sb.append(DDL.get(best));

        if (!top.isEmpty()) {
            sb.append("## EXAMPLES\n");
            for (Example ex : top) {
                sb.append(ex.qa).append('\n');
            }
        }

        sb.append("## QUESTION\n");
        sb.append(question).append('\n');
        return sb.toString();
    }

    // ================================================================
    // 7. Category picker — weighted scoring → single best match
    //
    //    Returns EMPLOYEE as default (lowest-risk fallback) when no
    //    keywords match.
    // ================================================================
    private static Category pickBestCategory(String question) {
        Category best  = Category.EMPLOYEE;
        int      score = 0;

        for (Map.Entry<Category, Map<String, Integer>> entry : WEIGHTED_KEYWORDS.entrySet()) {
            int s = 0;
            for (Map.Entry<String, Integer> kw : entry.getValue().entrySet()) {
                if (question.contains(kw.getKey())) {
                    s += kw.getValue();
                }
            }
            if (s > score) {
                score = s;
                best  = entry.getKey();
            }
        }

        return best;
    }

    // ================================================================
    // 8. Dynamic example selector
    //
    //    Score each example in the matched category by tag overlap.
    //    Return top N by score.  If no tags match, return the first
    //    example as a structural fallback (never return empty).
    // ================================================================
    private static List<Example> pickTopExamples(String question, Category cat, int topN) {
        // Collect (index, score) pairs for examples in this category
        List<int[]> scored = new ArrayList<>();

        for (int i = 0; i < EXAMPLE_POOL.size(); i++) {
            Example ex = EXAMPLE_POOL.get(i);
            if (ex.category != cat) continue;

            int s = 0;
            for (String tag : ex.tags) {
                if (question.contains(tag)) s += 2;
            }
            scored.add(new int[]{i, s});
        }

        if (scored.isEmpty()) return Collections.emptyList();

        // Sort descending by score
        scored.sort((a, b) -> b[1] - a[1]);

        List<Example> result = new ArrayList<>(topN);
        for (int j = 0; j < scored.size() && result.size() < topN; j++) {
            int[] pair = scored.get(j);
            // Include if score > 0, or as the one structural fallback
            if (pair[1] > 0 || result.isEmpty()) {
                result.add(EXAMPLE_POOL.get(pair[0]));
            }
        }

        return result;
    }

    // ================================================================
    // 9. Debug / diagnostics
    // ================================================================

    /** Returns the best-matched category name for the given question. */
    public static String getMatchedCategory(String question) {
        return pickBestCategory(question).name();
    }

    /**
     * Rough token estimate (Korean chars ≈ 1 token each via tiktoken cl100k).
     * Divide by 2.5 is a safe undercount for prompts mixing Korean + SQL.
     */
    public static int estimateTokens(String prompt) {
        return (int) (prompt.length() / 2.5);
    }
}