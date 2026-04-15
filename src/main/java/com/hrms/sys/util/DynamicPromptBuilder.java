package com.hrms.sys.util;

import java.util.*;

/**
 * Dynamic Prompt Builder v5 — Schema v4 Sync + Multi-Category JOIN
 *
 * Changes from v4:
 *   [1] Schema sync         : All DDL updated to match hr_erp_schema v4_fixed.
 *   [2] New tables           : leave_of_absence_request (휴직/복직 3단계 승인),
 *                              resign_request (퇴직 3단계 승인) added.
 *   [3] New columns          : job_position(meal/transport/position_allowance),
 *                              department(sort_order, closed_at),
 *                              employee(address, emergency_contact, bank_account),
 *                              attendance(note), salary(unpaid_leave_days, unpaid_deduction),
 *                              account role '최종승인자' added.
 *   [4] Position '사장'       : position_level 6 added (CEO/최종승인자).
 *   [5] View vs Raw table    : Clarified when views lack columns
 *                              (v_salary_summary has no meal/transport/position_allowance
 *                               or unpaid_leave_days/unpaid_deduction → use raw salary table).
 *   [6] Cross-domain JOIN    : Multi-category detection from v4 retained.
 *   [7] New keyword/examples : 휴직신청, 복직신청, 퇴직신청, 사장, 최종승인자 etc.
 */
public class DynamicPromptBuilder {

    private enum Category {
        EMPLOYEE, SALARY, ATTENDANCE, LEAVE, EVALUATION, ACCOUNT, SYSTEM
    }

    private static final Map<Category, Map<String, Integer>> WEIGHTED_KEYWORDS = new LinkedHashMap<>();

    static {
        Map<String, Integer> emp = new LinkedHashMap<>();
        for (String k : new String[]{"사원","대리","과장","차장","부장","사장","직급","직급별"}) emp.put(k, 3);
        for (String k : new String[]{"재직","퇴직","휴직","퇴사","복직"}) emp.put(k, 3);
        for (String k : new String[]{"정규직","계약직","파트타임"}) emp.put(k, 3);
        for (String k : new String[]{"발령","승진","전보","인사이동"}) emp.put(k, 3);
        for (String k : new String[]{"휴직신청","복직신청","퇴직신청","3단계","승인단계"}) emp.put(k, 3);
        for (String k : new String[]{"성별","생년","출생","나이","만나이","근속","입사"}) emp.put(k, 2);
        for (String k : new String[]{"부서장승인","HR담당자승인","최종승인","반려"}) emp.put(k, 2);
        for (String k : new String[]{"직원","인원","명단","부서","부서별","연봉","기본급","이름","검색"}) emp.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.EMPLOYEE, emp);

        Map<String, Integer> sal = new LinkedHashMap<>();
        for (String k : new String[]{"급여","월급","실수령","세전","세후","공제","수당","명세","미지급","지급완료","급여대기"}) sal.put(k, 3);
        for (String k : new String[]{"국민연금","건강보험","고용보험","소득세","식대","교통비","직책수당","장기요양"}) sal.put(k, 2);
        for (String k : new String[]{"무급공제","무급","결근공제","병가공제","unpaid"}) sal.put(k, 2);
        for (String k : new String[]{"지급","공제율","보험료"}) sal.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.SALARY, sal);

        Map<String, Integer> att = new LinkedHashMap<>();
        for (String k : new String[]{"출근","퇴근","지각","결근","근태","출퇴근","초과근무","야근","잔업","오버타임"}) att.put(k, 3);
        for (String k : new String[]{"근무시간","출장"}) att.put(k, 2);
        for (String k : new String[]{"근무"}) att.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.ATTENDANCE, att);

        Map<String, Integer> lev = new LinkedHashMap<>();
        for (String k : new String[]{"연차","반차","병가","경조사","공가","잔여연차","휴가일수","남은연차"}) lev.put(k, 3);
        for (String k : new String[]{"휴가신청","휴가승인","휴가반려"}) lev.put(k, 2);
        for (String k : new String[]{"휴가","잔여","사용"}) lev.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.LEAVE, lev);

        Map<String, Integer> eva = new LinkedHashMap<>();
        for (String k : new String[]{"평가","인사평가","고과","등급","S등급","A등급","B등급","C등급","D등급"}) eva.put(k, 3);
        for (String k : new String[]{"상반기평가","하반기평가","자기평가","상위평가","동료평가","최종확정"}) eva.put(k, 2);
        for (String k : new String[]{"점수","평가자"}) eva.put(k, 1);
        WEIGHTED_KEYWORDS.put(Category.EVALUATION, eva);

        Map<String, Integer> acc = new LinkedHashMap<>();
        for (String k : new String[]{"관리자","HR담당자","최종승인자","계정","잠금","잠긴","권한","username","아이디"}) acc.put(k, 3);
        for (String k : new String[]{"로그인","로그아웃","비밀번호","활성","비활성","차단","로그인실패"}) acc.put(k, 2);
        WEIGHTED_KEYWORDS.put(Category.ACCOUNT, acc);

        Map<String, Integer> sys = new LinkedHashMap<>();
        for (String k : new String[]{"감사로그","변경이력","감사","알림","공휴일"}) sys.put(k, 3);
        for (String k : new String[]{"로그","이력","안읽은","읽은","휴일","명절"}) sys.put(k, 2);
        WEIGHTED_KEYWORDS.put(Category.SYSTEM, sys);
    }

    private static final String HEADER =
    		"You are a MySQL 8.0 expert for an HR-ERP system.\n" +
    		        "Output ONLY a raw MySQL SELECT query ending with a semicolon.\n" +
    		        "No markdown fences, no explanation, no preamble.\n" +
    		        "Use ONLY columns defined in the DDL below. NEVER invent columns.\n" +
    		        "Default LIMIT: 100 (omit if aggregation only).\n\n" +
    		        "## FALLBACK RULE\n" +
    		        "If the question CANNOT be answered with the given schema,\n" +
    		        "output EXACTLY the single word:  불가능\n" +
    		        "Do NOT generate any SQL in that case.\n\n" +
    		        "## CRITICAL COLUMN CONSTRAINTS\n" +
    		        "- `role`            : EXISTS ONLY in `account`. Views have NO `role` column.\n" +
    		        "- `dept_name`       : EXISTS ONLY in `department` and views. " +
    		        "NEVER use e.dept_name from `employee`. Use v_employee_full or JOIN department.\n" +
    		        "- `position_name`   : Use for 사장/부장 filtering...\n" +
    		        "- `emp_type`        : ONLY for 정규직/계약직/파트타임. NEVER for position titles.\n" +
    		        "- `manager_id`      : EXISTS ONLY in `department`. Views have NO `manager_id`.\n" +
    		        "- `age`             : NO such column. Use TIMESTAMPDIFF(YEAR, birth_date, CURDATE()).\n" +
    		        "- `gender`          : CHAR(1) values are 'M' (남) and 'F' (여). NEVER 남자/여자/남/여.\n" +
    		        "- `v_employee_full` : does NOT contain dept_id, role, manager_id, locked_at, address, bank_account.\n" +
    		        "- `v_salary_summary`: payment state column is `salary_status` (NOT `status`).\n" +
    		        "                      does NOT contain meal_allowance, transport_allowance, position_allowance,\n" +
    		        "                      unpaid_leave_days, unpaid_deduction. Use raw `salary` table for these.\n" +
    		        "- `v_leave_status`  : leave state column is `leave_status` (NOT `status`).\n" +
    		        "                      CRITICAL: DO NOT use this view for checking total/remaining leave days. It omits employees with zero requests.\n" +
    		        "- `evaluator_name`  : EXISTS ONLY in `v_evaluation_result`. Raw `evaluation` has only `evaluator_id`.\n" +
    		        "- 3-step approval tables (leave_of_absence_request, resign_request):\n" +
    		        "  status flow: 대기 → 부서장승인 → HR담당자승인 → 최종승인 | 반려\n\n" +
        "## FK RELATION MAP\n" +
        "employee.dept_id          -> department.dept_id\n" +
        "employee.position_id      -> job_position.position_id\n" +
        "department.manager_id     -> employee.emp_id\n" +
        "account.emp_id            -> employee.emp_id\n" +
        "salary.emp_id             -> employee.emp_id\n" +
        "attendance.emp_id         -> employee.emp_id\n" +
        "overtime_request.emp_id   -> employee.emp_id\n" +
        "overtime_request.approver_id -> employee.emp_id\n" +
        "leave_request.emp_id      -> employee.emp_id\n" +
        "leave_request.approver_id -> employee.emp_id\n" +
        "annual_leave.emp_id       -> employee.emp_id\n" +
        "evaluation.emp_id         -> employee.emp_id\n" +
        "evaluation.evaluator_id   -> employee.emp_id\n" +
        "personnel_history.emp_id  -> employee.emp_id\n" +
        "personnel_history.approved_by -> employee.emp_id\n" +
        "leave_of_absence_request.emp_id          -> employee.emp_id\n" +
        "leave_of_absence_request.dept_manager_id -> employee.emp_id\n" +
        "leave_of_absence_request.hr_manager_id   -> employee.emp_id\n" +
        "leave_of_absence_request.president_id    -> employee.emp_id\n" +
        "resign_request.emp_id          -> employee.emp_id\n" +
        "resign_request.dept_manager_id -> employee.emp_id\n" +
        "resign_request.hr_manager_id   -> employee.emp_id\n" +
        "resign_request.president_id    -> employee.emp_id\n" +
        "audit_log.actor_id        -> employee.emp_id  (LEFT JOIN; NULL = system action)\n" +
        "notification.emp_id       -> employee.emp_id\n\n" +
        "## MULTI-TABLE JOIN RULES\n" +
        "- All domain tables connect to `employee` via `emp_id`. Use `employee` as the HUB.\n" +
        "- For position filtering with domain data: JOIN employee -> job_position ON position_id.\n" +
        "- For dept filtering with domain data: JOIN employee -> department ON dept_id.\n" +
        "- When combining 2+ domain tables, JOIN each to employee separately.\n" +
        "- Use subqueries or CTEs when aggregation is needed before joining.\n" +
        "- Prefer views when they already contain needed columns.\n" +
        "  But for cross-domain queries or detailed columns, use raw tables with explicit JOINs.\n\n" +
        "## VIEWS  (prefer when all needed columns exist)\n" +
        "v_employee_full    : emp_id, emp_name, emp_no, status, emp_type, base_salary, hire_date, resign_date, birth_date, gender, email, phone, dept_name, position_name, position_level\n" +
        "v_salary_summary   : emp_id, emp_name, emp_no, dept_name, position_name, salary_year, salary_month, base_salary, gross_salary, total_deduction, net_salary, overtime_pay, salary_status, pay_date\n" +
        "  WARNING: MISSING from view: meal_allowance, transport_allowance, position_allowance, unpaid_leave_days, unpaid_deduction -> use raw salary JOIN employee\n" +
        "v_leave_status     : emp_id, emp_name, emp_no, dept_name, position_name, leave_id, leave_type, half_type, start_date, end_date, days, reason, leave_status, approved_at, reject_reason, leave_year, total_days, used_days, remain_days\n" +
        "v_evaluation_result: emp_id, emp_name, emp_no, dept_name, position_name, eval_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, confirmed_at, evaluator_name\n\n";

    private static final String SHARED_DDL =
        "```sql\n" +
        "CREATE TABLE job_position (\n" +
        "  position_id         INT          PRIMARY KEY,\n" +
        "  position_name       VARCHAR(20)  NOT NULL,   -- 사원/대리/과장/차장/부장/사장\n" +
        "  position_level      INT          NOT NULL,   -- 1=사원 ~ 5=부장, 6=사장\n" +
        "  base_salary         INT          NOT NULL,\n" +
        "  meal_allowance      INT          NOT NULL DEFAULT 0,\n" +
        "  transport_allowance INT          NOT NULL DEFAULT 0,\n" +
        "  position_allowance  INT          NOT NULL DEFAULT 0,\n" +
        "  is_active           TINYINT(1)   NOT NULL\n" +
        ");\n\n" +
        "CREATE TABLE department (\n" +
        "  dept_id        INT          PRIMARY KEY,\n" +
        "  dept_name      VARCHAR(50)  NOT NULL,\n" +
        "  parent_dept_id INT,\n" +
        "  manager_id     INT,\n" +
        "  dept_level     INT          NOT NULL,\n" +
        "  sort_order     INT          NOT NULL DEFAULT 0,\n" +
        "  is_active      TINYINT(1)   NOT NULL,\n" +
        "  closed_at      DATE\n" +
        ");\n\n" +
        "CREATE TABLE employee (\n" +
        "  emp_id            INT          PRIMARY KEY,\n" +
        "  emp_name          VARCHAR(20)  NOT NULL,\n" +
        "  emp_no            VARCHAR(20)  NOT NULL UNIQUE,\n" +
        "  dept_id           INT          NOT NULL,\n" +
        "  position_id       INT          NOT NULL,\n" +
        "  hire_date         DATE         NOT NULL,\n" +
        "  resign_date       DATE,\n" +
        "  emp_type          VARCHAR(10)  NOT NULL,\n" +
        "  status            VARCHAR(10)  NOT NULL,\n" +
        "  base_salary       INT          NOT NULL,\n" +
        "  birth_date        DATE,\n" +
        "  gender            CHAR(1),\n" +
        "  address           VARCHAR(200),\n" +
        "  emergency_contact VARCHAR(20),\n" +
        "  bank_account      VARCHAR(30),\n" +
        "  email             VARCHAR(100),\n" +
        "  phone             VARCHAR(20)\n" +
        ");\n" +
        "```\n\n";

    private static final Map<Category, String> DOMAIN_DDL = new LinkedHashMap<>();
    private static final Map<Category, String> RULES = new LinkedHashMap<>();

    static {
        DOMAIN_DDL.put(Category.EMPLOYEE,
            "```sql\n" +
            "CREATE TABLE personnel_history (\n" +
            "  history_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  change_type VARCHAR(20) NOT NULL, -- 발령/승진/전보/퇴직/복직\n" +
            "  from_dept_id INT, to_dept_id INT, from_position_id INT, to_position_id INT,\n" +
            "  change_date DATE NOT NULL, reason VARCHAR(200), approved_by INT\n" +
            ");\n" +
            "CREATE TABLE leave_of_absence_request (\n" +
            "  request_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  leave_type VARCHAR(10) NOT NULL, -- 휴직/복직\n" +
            "  start_date DATE NOT NULL, end_date DATE,\n" +
            "  reason VARCHAR(500),\n" +
            "  status VARCHAR(20) NOT NULL DEFAULT '대기', -- 대기/부서장승인/HR담당자승인/최종승인/반려\n" +
            "  dept_manager_id INT, dept_approved_at DATETIME,\n" +
            "  hr_manager_id INT, hr_approved_at DATETIME,\n" +
            "  president_id INT, president_approved_at DATETIME,\n" +
            "  reject_reason VARCHAR(200)\n" +
            ");\n" +
            "CREATE TABLE resign_request (\n" +
            "  request_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  resign_date DATE NOT NULL, reason VARCHAR(500),\n" +
            "  status VARCHAR(20) NOT NULL DEFAULT '대기', -- 대기/부서장승인/HR담당자승인/최종승인/반려\n" +
            "  dept_manager_id INT, dept_approved_at DATETIME,\n" +
            "  hr_manager_id INT, hr_approved_at DATETIME,\n" +
            "  president_id INT, president_approved_at DATETIME,\n" +
            "  reject_reason VARCHAR(200)\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.SALARY,
            "```sql\n" +
            "CREATE TABLE salary (\n" +
            "  salary_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  salary_year INT NOT NULL, salary_month INT NOT NULL,\n" +
            "  base_salary INT NOT NULL DEFAULT 0,\n" +
            "  meal_allowance INT NOT NULL DEFAULT 0, transport_allowance INT NOT NULL DEFAULT 0,\n" +
            "  position_allowance INT NOT NULL DEFAULT 0, overtime_pay INT NOT NULL DEFAULT 0,\n" +
            "  other_allowance INT NOT NULL DEFAULT 0, gross_salary INT NOT NULL DEFAULT 0,\n" +
            "  national_pension INT NOT NULL DEFAULT 0, health_insurance INT NOT NULL DEFAULT 0,\n" +
            "  long_term_care INT NOT NULL DEFAULT 0, employment_insurance INT NOT NULL DEFAULT 0,\n" +
            "  unpaid_leave_days DECIMAL(4,1) NOT NULL DEFAULT 0, -- 무급 공제 일수\n" +
            "  unpaid_deduction INT NOT NULL DEFAULT 0, -- 무급 공제액\n" +
            "  income_tax INT NOT NULL DEFAULT 0, local_income_tax INT NOT NULL DEFAULT 0,\n" +
            "  total_deduction INT NOT NULL DEFAULT 0, net_salary INT NOT NULL DEFAULT 0,\n" +
            "  pay_date DATE, status VARCHAR(10) NOT NULL DEFAULT '대기' -- 대기/완료\n" +
            ");\n" +
            "CREATE TABLE deduction_rate (\n" +
            "  rate_id INT PRIMARY KEY, target_year INT NOT NULL UNIQUE,\n" +
            "  national_pension_rate DECIMAL(6,5), health_insurance_rate DECIMAL(6,5),\n" +
            "  long_term_care_rate DECIMAL(6,5), employment_insurance_rate DECIMAL(6,5)\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.ATTENDANCE,
            "```sql\n" +
            "CREATE TABLE attendance (\n" +
            "  att_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  work_date DATE NOT NULL, check_in TIME, check_out TIME,\n" +
            "  work_hours DECIMAL(4,2), overtime_hours DECIMAL(4,2) NOT NULL DEFAULT 0,\n" +
            "  status VARCHAR(20) NOT NULL, -- 출근/지각/결근/휴가/출장\n" +
            "  note VARCHAR(200)\n" +
            ");\n" +
            "CREATE TABLE overtime_request (\n" +
            "  ot_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  ot_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,\n" +
            "  ot_hours DECIMAL(4,2) NOT NULL, reason VARCHAR(300),\n" +
            "  status VARCHAR(10) NOT NULL, -- 대기/승인/반려\n" +
            "  approver_id INT, approved_at DATETIME\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.LEAVE,
            "```sql\n" +
            "CREATE TABLE leave_request (\n" +
            "  leave_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  leave_type VARCHAR(20) NOT NULL, -- 연차/반차/병가/경조사/공가\n" +
            "  half_type VARCHAR(10), -- 오전/오후 (반차 only)\n" +
            "  start_date DATE NOT NULL, end_date DATE NOT NULL,\n" +
            "  days DECIMAL(4,1) NOT NULL, reason VARCHAR(500),\n" +
            "  status VARCHAR(10) NOT NULL, -- 대기/승인/반려/취소\n" +
            "  approver_id INT, approved_at DATETIME, reject_reason VARCHAR(200)\n" +
            ");\n" +
            "CREATE TABLE annual_leave (\n" +
            "  al_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  leave_year INT NOT NULL, total_days DECIMAL(4,1) NOT NULL,\n" +
            "  used_days DECIMAL(4,1) NOT NULL, remain_days DECIMAL(4,1) NOT NULL\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.EVALUATION,
            "```sql\n" +
            "CREATE TABLE evaluation (\n" +
            "  eval_id INT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  eval_year INT NOT NULL, eval_period VARCHAR(10) NOT NULL, -- 상반기/하반기/연간\n" +
            "  eval_type VARCHAR(20) NOT NULL, -- 자기평가/상위평가/동료평가\n" +
            "  total_score DECIMAL(5,2), grade VARCHAR(5), -- S/A/B/C/D\n" +
            "  eval_comment TEXT, eval_status VARCHAR(10) NOT NULL, -- 작성중/최종확정\n" +
            "  evaluator_id INT, confirmed_at DATETIME\n" +
            ");\n" +
            "CREATE TABLE evaluation_item (\n" +
            "  item_id INT PRIMARY KEY, eval_id INT NOT NULL,\n" +
            "  item_name VARCHAR(50) NOT NULL, score DECIMAL(5,2) NOT NULL, max_score DECIMAL(5,2) NOT NULL\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.ACCOUNT,
            "```sql\n" +
            "CREATE TABLE account (\n" +
            "  account_id INT PRIMARY KEY, emp_id INT NOT NULL UNIQUE,\n" +
            "  username VARCHAR(50) NOT NULL UNIQUE,\n" +
            "  role VARCHAR(20) NOT NULL, -- 관리자/HR담당자/일반/최종승인자\n" +
            "  is_active TINYINT(1) NOT NULL, login_attempts INT NOT NULL DEFAULT 0,\n" +
            "  last_login DATETIME, password_changed_at DATETIME,\n" +
            "  locked_at DATETIME -- NULL = not locked\n" +
            ");\n```\n\n");

        DOMAIN_DDL.put(Category.SYSTEM,
            "```sql\n" +
            "CREATE TABLE notification (\n" +
            "  noti_id BIGINT PRIMARY KEY, emp_id INT NOT NULL,\n" +
            "  noti_type VARCHAR(30) NOT NULL, ref_table VARCHAR(50), ref_id INT,\n" +
            "  message VARCHAR(300) NOT NULL, is_read TINYINT(1) NOT NULL DEFAULT 0,\n" +
            "  read_at DATETIME, created_at DATETIME NOT NULL\n" +
            ");\n" +
            "CREATE TABLE audit_log (\n" +
            "  log_id BIGINT PRIMARY KEY, actor_id INT,\n" +
            "  target_table VARCHAR(50) NOT NULL, target_id INT NOT NULL,\n" +
            "  action VARCHAR(10) NOT NULL, -- INSERT/UPDATE/DELETE\n" +
            "  column_name VARCHAR(50), old_value TEXT, new_value TEXT,\n" +
            "  created_at DATETIME NOT NULL\n" +
            ");\n" +
            "CREATE TABLE public_holiday (\n" +
            "  holiday_id INT PRIMARY KEY, holiday_date DATE NOT NULL,\n" +
            "  holiday_name VARCHAR(50) NOT NULL, holiday_year INT NOT NULL\n" +
            ");\n```\n\n");

        // RULES
        RULES.put(Category.EMPLOYEE,
            "## RULES (EMPLOYEE)\n" +
            "- GENERAL EMPLOYEE QUERY: ALWAYS use v_employee_full. NEVER SELECT dept_name or position_name FROM employee directly.\n" +
            "- NAME SEARCH: Use emp_name LIKE '%검색어%' ONLY. NEVER add gender filter for name searches.\n" +
            "- 성씨/이름 검색 시 gender 조건 절대 추가 금지.\n" +
            "- ADDRESS SEARCH: v_employee_full v JOIN employee e ON v.emp_id = e.emp_id 사용.\n" +
            "  SELECT 시 반드시 v.emp_name, v.dept_name 등 접두사 명시. 접두사 없으면 ambiguous 에러 발생.\n" +
            "- 지역 검색: WHERE e.address LIKE '%지역명%' 패턴 사용.\n" +
            "- Position levels: 1=사원, 2=대리, 3=과장, 4=차장, 5=부장, 6=사장\n" +
            "- Position filter: WHERE position_name = '부장' -> use v_employee_full or JOIN job_position\n" +
            "- Emp type filter: WHERE emp_type = '정규직'\n" +
            "- Status filter: WHERE status = '재직'\n" +
            "- Age: TIMESTAMPDIFF(YEAR, birth_date, CURDATE())\n" +
            "- Tenure: TIMESTAMPDIFF(YEAR, hire_date, CURDATE())\n" +
            "- Gender: gender = 'M' (male) | 'F' (female)\n" +
            "- Dept head: FROM department d JOIN employee e ON d.manager_id = e.emp_id\n" +
            "- 휴직/복직/퇴직 신청 status: 대기 -> 부서장승인 -> HR담당자승인 -> 최종승인 | 반려\n" +
            "- leave_of_absence_request: 휴직(leave_type='휴직') / 복직(leave_type='복직')\n" +
            "- resign_request: 퇴직 신청 (3단계 승인)\n\n");

        RULES.put(Category.SALARY,
            "## RULES (SALARY)\n" +
            "- View v_salary_summary: use salary_status (NOT status) for payment state.\n" +
            "  WARNING: View does NOT have meal_allowance, transport_allowance, position_allowance,\n" +
            "  unpaid_leave_days, unpaid_deduction. Use raw salary table + JOIN for these.\n" +
            "- Raw table salary: use status for payment state.\n" +
            "- Paid: salary_status = '완료' (view) | status = '완료' (raw)\n" +
            "- Pending: salary_status = '대기' (view) | status = '대기' (raw)\n" +
            "- This month: salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE())\n" +
            "- 무급 공제: unpaid_leave_days (일수), unpaid_deduction (금액) — raw salary only\n\n");

        RULES.put(Category.ATTENDANCE,
            "## RULES (ATTENDANCE)\n" +
            "- No views for attendance/overtime. Always JOIN employee ON emp_id.\n" +
            "- Late: status = '지각'\n" +
            "- Absent: status = '결근'\n" +
            "- Overtime approved: overtime_request.status = '승인'\n" +
            "- This month: YEAR(work_date) = YEAR(CURDATE()) AND MONTH(work_date) = MONTH(CURDATE())\n\n");

        RULES.put(Category.LEAVE,
                "## RULES (LEAVE)\n" +
                "- View v_leave_status: use leave_status (NOT status) for leave state. Use ONLY for requested leave history.\n" +
                "- Raw table: use status.\n" +
                "- Approved: leave_status = '승인' (view) | status = '승인' (raw)\n" +
                "- Pending: leave_status = '대기' (view) | status = '대기' (raw)\n" +
                "- CRITICAL: For remaining/total/used leave days, ALWAYS join `annual_leave` with `employee` or `v_employee_full`.\n\n");

        RULES.put(Category.EVALUATION,
            "## RULES (EVALUATION)\n" +
            "- evaluator_name: EXISTS ONLY in v_evaluation_result. Raw table has only evaluator_id.\n" +
            "- Confirmed: eval_status = '최종확정'\n" +
            "- Grade: grade = 'S' (or A/B/C/D)\n\n");

        RULES.put(Category.ACCOUNT,
            "## RULES (ACCOUNT)\n" +
            "- role EXISTS ONLY in account. Views do NOT have role.\n" +
            "- Role: 관리자 / HR담당자 / 일반 / 최종승인자\n" +
            "- Locked: locked_at IS NOT NULL\n" +
            "- Active: is_active = 1 AND locked_at IS NULL\n" +
            "- Always JOIN employee for emp_name, email, phone.\n\n");

        RULES.put(Category.SYSTEM,
            "## RULES (SYSTEM)\n" +
            "- Unread: is_read = 0\n" +
            "- Audit actor: LEFT JOIN employee ON actor_id = emp_id (NULL = system)\n" +
            "- audit_log tracks: employee(base_salary/status/resign_date), account(role/is_active), annual_leave(used_days/remain_days), salary(status)\n\n");
    }

    // ================================================================
    // 5. Example pool
    // ================================================================
    private static class Example {
        final Category primaryCategory;
        final Category[] categories;
        final String[] tags;
        final String qa;
        final boolean isCrossDomain;

        Example(Category cat, String[] tags, String qa) {
            this.primaryCategory = cat; this.categories = new Category[]{cat};
            this.tags = tags; this.qa = qa; this.isCrossDomain = false;
        }
        Example(Category primary, Category[] cats, String[] tags, String qa) {
            this.primaryCategory = primary; this.categories = cats;
            this.tags = tags; this.qa = qa; this.isCrossDomain = true;
        }
    }

    private static final List<Example> EXAMPLE_POOL = new ArrayList<>();

    static {
        // ── EMPLOYEE ──
        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부장","직급","과장","차장"},
            "Q: 부장 목록\nA: SELECT emp_name, dept_name, position_name, email, phone\n   FROM v_employee_full\n   WHERE position_name = '부장' AND status = '재직';\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부서장","팀장","매니저"},
            "Q: 부서장 목록\nA: SELECT e.emp_name, d.dept_name, p.position_name\n   FROM department d\n   JOIN employee e ON d.manager_id = e.emp_id\n   JOIN job_position p ON e.position_id = p.position_id\n   WHERE d.is_active = 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"나이","연령","만나이","세","30","40"},
            "Q: 30세 이상 40세 이하 재직 직원\nA: SELECT emp_name, dept_name, TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS age\n   FROM v_employee_full\n   WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) BETWEEN 30 AND 40;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"발령","승진","전보","인사이동"},
            "Q: 올해 발령 이력\nA: SELECT e.emp_name, ph.change_type, ph.change_date, ph.reason\n   FROM personnel_history ph\n   JOIN employee e ON ph.emp_id = e.emp_id\n   WHERE YEAR(ph.change_date) = YEAR(CURDATE())\n   ORDER BY ph.change_date DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"부서","직원수","인원수","통계","부서별"},
            "Q: 부서별 재직 직원 수\nA: SELECT dept_name, COUNT(*) AS emp_count\n   FROM v_employee_full\n   WHERE status = '재직'\n   GROUP BY dept_name\n   ORDER BY emp_count DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"근속","년","경력","5년"},
            "Q: 근속 5년 이상 직원\nA: SELECT emp_name, dept_name, hire_date, TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) AS years_worked\n   FROM v_employee_full\n   WHERE status = '재직' AND TIMESTAMPDIFF(YEAR, hire_date, CURDATE()) >= 5\n   ORDER BY hire_date;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"휴직신청","휴직","대기","승인","복직신청"},
            "Q: 현재 대기 중인 휴직 신청 목록\nA: SELECT e.emp_name, d.dept_name, loa.leave_type, loa.start_date, loa.end_date, loa.reason, loa.status\n   FROM leave_of_absence_request loa\n   JOIN employee e ON loa.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   WHERE loa.status NOT IN ('최종승인', '반려')\n   ORDER BY loa.created_at;\n"));

        EXAMPLE_POOL.add(new Example(Category.EMPLOYEE,
            new String[]{"퇴직신청","퇴직","대기","승인단계"},
            "Q: 퇴직 신청 현황 (승인 단계별)\nA: SELECT e.emp_name, d.dept_name, rr.resign_date, rr.reason, rr.status,\n          dm.emp_name AS dept_manager, hr.emp_name AS hr_manager, pr.emp_name AS president\n   FROM resign_request rr\n   JOIN employee e ON rr.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   LEFT JOIN employee dm ON rr.dept_manager_id = dm.emp_id\n   LEFT JOIN employee hr ON rr.hr_manager_id = hr.emp_id\n   LEFT JOIN employee pr ON rr.president_id = pr.emp_id\n   ORDER BY rr.created_at DESC;\n"));

        // ── SALARY ──
        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"급여","이번달","현황","월급"},
            "Q: 이번달 급여 현황\nA: SELECT emp_name, dept_name, gross_salary, total_deduction, net_salary, salary_status\n   FROM v_salary_summary\n   WHERE salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"미지급","대기","미완료","지급안된"},
            "Q: 미지급 급여 목록\nA: SELECT emp_name, dept_name, salary_year, salary_month, net_salary, salary_status\n   FROM v_salary_summary\n   WHERE salary_status = '대기';\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"공제","국민연금","건강보험","고용보험","항목"},
            "Q: 이번달 공제 내역\nA: SELECT emp_name, national_pension, health_insurance, employment_insurance, income_tax, total_deduction\n   FROM v_salary_summary\n   WHERE salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"식대","교통비","직책수당","수당","항목별","상세"},
            "Q: 이번달 직원별 수당 상세 내역\nA: SELECT e.emp_name, d.dept_name, s.meal_allowance, s.transport_allowance,\n          s.position_allowance, s.overtime_pay, s.other_allowance\n   FROM salary s\n   JOIN employee e ON s.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   WHERE s.salary_year = YEAR(CURDATE()) AND s.salary_month = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new String[]{"무급","무급공제","결근공제","병가공제"},
            "Q: 무급 공제가 발생한 직원\nA: SELECT e.emp_name, d.dept_name, s.unpaid_leave_days, s.unpaid_deduction, s.gross_salary, s.net_salary\n   FROM salary s\n   JOIN employee e ON s.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   WHERE s.unpaid_leave_days > 0 AND s.salary_year = YEAR(CURDATE());\n"));

        // ── ATTENDANCE ──
        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"지각","이번달","이번주"},
            "Q: 이번달 지각 직원\nA: SELECT e.emp_name, a.work_date, a.check_in\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   WHERE a.status = '지각' AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"초과근무","야근","잔업","승인","오버타임"},
            "Q: 이번달 승인된 초과근무 건수 및 총 시간\nA: SELECT COUNT(*) AS approved_count, SUM(ot_hours) AS total_ot_hours\n   FROM overtime_request\n   WHERE status = '승인' AND YEAR(ot_date) = YEAR(CURDATE()) AND MONTH(ot_date) = MONTH(CURDATE());\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new String[]{"결근","이번달"},
            "Q: 이번달 결근 현황\nA: SELECT e.emp_name, a.work_date\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   WHERE a.status = '결근' AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE());\n"));

        // ── LEAVE ──
        EXAMPLE_POOL.add(new Example(Category.LEAVE,
                new String[]{"잔여","남은","연차","3일"},
                "Q: 잔여 연차 3일 이하 직원\nA: SELECT v.emp_name, v.dept_name, al.remain_days\n   FROM v_employee_full v\n   JOIN annual_leave al ON v.emp_id = al.emp_id\n   WHERE al.leave_year = YEAR(CURDATE()) AND v.status = '재직' AND al.remain_days <= 3;\n"));
        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new String[]{"대기","신청","승인대기","휴가"},
            "Q: 승인 대기 중인 휴가 신청\nA: SELECT emp_name, dept_name, leave_type, start_date, end_date, days\n   FROM v_leave_status\n   WHERE leave_status = '대기';\n"));

        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new String[]{"병가","경조사"},
            "Q: 올해 병가 사용 현황\nA: SELECT emp_name, dept_name, start_date, end_date, days\n   FROM v_leave_status\n   WHERE leave_type = '병가' AND leave_year = YEAR(CURDATE()) AND leave_status = '승인';\n"));

        // ── EVALUATION ──
        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"S등급","하반기","상반기","등급"},
            "Q: 2024년 하반기 S등급 직원\nA: SELECT emp_name, dept_name, total_score, grade\n   FROM v_evaluation_result\n   WHERE eval_year = 2024 AND eval_period = '하반기' AND grade = 'S';\n"));

        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"평가자","건수","담당"},
            "Q: 평가자별 평가 건수\nA: SELECT evaluator_name, COUNT(*) AS eval_count\n   FROM v_evaluation_result\n   WHERE eval_status = '최종확정'\n   GROUP BY evaluator_name ORDER BY eval_count DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new String[]{"부서","평균","점수"},
            "Q: 부서별 평균 평가 점수\nA: SELECT dept_name, ROUND(AVG(total_score), 2) AS avg_score\n   FROM v_evaluation_result\n   WHERE eval_status = '최종확정'\n   GROUP BY dept_name ORDER BY avg_score DESC;\n"));

        // ── ACCOUNT ──
        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"관리자","목록"},
            "Q: 관리자 목록\nA: SELECT e.emp_name, e.email, e.phone, a.username, a.role\n   FROM account a\n   JOIN employee e ON a.emp_id = e.emp_id\n   WHERE a.role = '관리자' AND a.is_active = 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"잠긴","잠금","차단","계정","로그인실패"},
            "Q: 잠긴 계정 목록\nA: SELECT a.account_id, e.emp_name, a.username, a.login_attempts, a.locked_at\n   FROM account a\n   JOIN employee e ON a.emp_id = e.emp_id\n   WHERE a.locked_at IS NOT NULL;\n"));

        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new String[]{"권한","역할","현황","HR담당자","최종승인자"},
            "Q: 권한별 계정 수\nA: SELECT role, COUNT(*) AS cnt FROM account WHERE is_active = 1 GROUP BY role;\n"));

        // ── SYSTEM ──
        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"공휴일","휴일","명절"},
            "Q: 올해 공휴일 목록\nA: SELECT holiday_date, holiday_name FROM public_holiday\n   WHERE holiday_year = YEAR(CURDATE()) ORDER BY holiday_date;\n"));

        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"감사","로그","이력","변경"},
            "Q: 최근 감사로그 10건\nA: SELECT al.created_at, e.emp_name AS actor, al.target_table, al.action, al.column_name, al.old_value, al.new_value\n   FROM audit_log al\n   LEFT JOIN employee e ON al.actor_id = e.emp_id\n   ORDER BY al.created_at DESC LIMIT 10;\n"));

        EXAMPLE_POOL.add(new Example(Category.SYSTEM,
            new String[]{"알림","안읽은","읽지않은"},
            "Q: 안읽은 알림이 많은 직원\nA: SELECT e.emp_name, COUNT(*) AS unread_count\n   FROM notification n\n   JOIN employee e ON n.emp_id = e.emp_id\n   WHERE n.is_read = 0\n   GROUP BY n.emp_id, e.emp_name ORDER BY unread_count DESC;\n"));

        // ── CROSS-DOMAIN ──
        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new Category[]{Category.EMPLOYEE, Category.ATTENDANCE},
            new String[]{"부장","출근","빠른","가장","이른","먼저"},
            "Q: 부장들 중 오늘 출근시간이 가장 빠른 사람\nA: SELECT e.emp_name, d.dept_name, p.position_name, a.check_in\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   JOIN job_position p ON e.position_id = p.position_id\n   JOIN department d ON e.dept_id = d.dept_id\n   WHERE p.position_name = '부장' AND e.status = '재직'\n     AND a.work_date = CURDATE() AND a.check_in IS NOT NULL\n   ORDER BY a.check_in ASC LIMIT 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new Category[]{Category.EMPLOYEE, Category.ATTENDANCE},
            new String[]{"부서별","지각","횟수","직급","많은"},
            "Q: 부서별 직급별 이번달 지각 횟수\nA: SELECT d.dept_name, p.position_name, COUNT(*) AS late_count\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   JOIN job_position p ON e.position_id = p.position_id\n   WHERE a.status = '지각' AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE())\n   GROUP BY d.dept_name, p.position_name ORDER BY late_count DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new Category[]{Category.EMPLOYEE, Category.ATTENDANCE},
            new String[]{"과장","퇴근","늦은","야근","가장"},
            "Q: 이번달 과장들 중 가장 늦게 퇴근한 사람 TOP 5\nA: SELECT e.emp_name, d.dept_name, a.work_date, a.check_out\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   JOIN job_position p ON e.position_id = p.position_id\n   JOIN department d ON e.dept_id = d.dept_id\n   WHERE p.position_name = '과장' AND e.status = '재직' AND a.check_out IS NOT NULL\n     AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE())\n   ORDER BY a.check_out DESC LIMIT 5;\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new Category[]{Category.EMPLOYEE, Category.SALARY},
            new String[]{"부장","급여","실수령","높은","가장"},
            "Q: 부장들 중 이번달 실수령액이 가장 높은 사람\nA: SELECT v.emp_name, v.dept_name, v.position_name, v.gross_salary, v.total_deduction, v.net_salary\n   FROM v_salary_summary v\n   WHERE v.position_name = '부장' AND v.salary_year = YEAR(CURDATE()) AND v.salary_month = MONTH(CURDATE())\n   ORDER BY v.net_salary DESC LIMIT 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new Category[]{Category.EMPLOYEE, Category.SALARY},
            new String[]{"부서","평균","급여","비교","직급"},
            "Q: 부서별 직급별 평균 실수령액\nA: SELECT v.dept_name, v.position_name, COUNT(*) AS cnt, ROUND(AVG(v.net_salary)) AS avg_net\n   FROM v_salary_summary v\n   WHERE v.salary_year = YEAR(CURDATE()) AND v.salary_month = MONTH(CURDATE())\n   GROUP BY v.dept_name, v.position_name ORDER BY v.dept_name, avg_net DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.LEAVE,
                new Category[]{Category.EMPLOYEE, Category.LEAVE},
                new String[]{"과장","연차","잔여","남은"},
                "Q: 과장들의 잔여 연차 현황\nA: SELECT v.emp_name, v.dept_name, v.position_name, al.total_days, al.used_days, al.remain_days\n   FROM v_employee_full v\n   JOIN annual_leave al ON v.emp_id = al.emp_id\n   WHERE v.position_name = '과장' AND v.status = '재직' AND al.leave_year = YEAR(CURDATE());\n"));
        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new Category[]{Category.EMPLOYEE, Category.EVALUATION},
            new String[]{"부장","평가","S등급","A등급","우수"},
            "Q: 부장들 중 S등급 받은 사람\nA: SELECT v.emp_name, v.dept_name, v.position_name, v.eval_year, v.eval_period, v.total_score, v.grade\n   FROM v_evaluation_result v\n   WHERE v.position_name = '부장' AND v.grade = 'S' AND v.eval_status = '최종확정';\n"));

        EXAMPLE_POOL.add(new Example(Category.ATTENDANCE,
            new Category[]{Category.EMPLOYEE, Category.ATTENDANCE, Category.SALARY},
            new String[]{"야근","초과근무","수당","많은","높은"},
            "Q: 이번달 초과근무 시간이 가장 많은 직원과 해당 초과근무수당\nA: SELECT e.emp_name, d.dept_name, p.position_name,\n          SUM(a.overtime_hours) AS total_ot_hours, s.overtime_pay\n   FROM attendance a\n   JOIN employee e ON a.emp_id = e.emp_id\n   JOIN department d ON e.dept_id = d.dept_id\n   JOIN job_position p ON e.position_id = p.position_id\n   LEFT JOIN salary s ON e.emp_id = s.emp_id\n     AND s.salary_year = YEAR(CURDATE()) AND s.salary_month = MONTH(CURDATE())\n   WHERE YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE())\n   GROUP BY e.emp_id, e.emp_name, d.dept_name, p.position_name, s.overtime_pay\n   ORDER BY total_ot_hours DESC LIMIT 1;\n"));

        EXAMPLE_POOL.add(new Example(Category.LEAVE,
            new Category[]{Category.EMPLOYEE, Category.LEAVE, Category.ATTENDANCE},
            new String[]{"지각","많은","연차","적은","잔여"},
            "Q: 이번달 지각이 3회 이상이면서 잔여 연차가 5일 이하인 직원\nA: SELECT e.emp_name, d.dept_name, late_stats.late_count, al.remain_days\n   FROM employee e\n   JOIN department d ON e.dept_id = d.dept_id\n   JOIN (SELECT emp_id, COUNT(*) AS late_count FROM attendance\n         WHERE status = '지각' AND YEAR(work_date) = YEAR(CURDATE()) AND MONTH(work_date) = MONTH(CURDATE())\n         GROUP BY emp_id HAVING COUNT(*) >= 3\n   ) late_stats ON e.emp_id = late_stats.emp_id\n   JOIN annual_leave al ON e.emp_id = al.emp_id AND al.leave_year = YEAR(CURDATE())\n   WHERE al.remain_days <= 5 AND e.status = '재직';\n"));

        EXAMPLE_POOL.add(new Example(Category.EVALUATION,
            new Category[]{Category.EMPLOYEE, Category.EVALUATION, Category.SALARY},
            new String[]{"평가","등급","급여","비교","높은","낮은","연봉"},
            "Q: S등급 받은 직원들의 평균 급여와 D등급 직원들의 평균 급여 비교\nA: SELECT ev.grade, COUNT(DISTINCT ev.emp_id) AS emp_count, ROUND(AVG(s.net_salary)) AS avg_net_salary\n   FROM evaluation ev\n   JOIN salary s ON ev.emp_id = s.emp_id\n     AND s.salary_year = YEAR(CURDATE()) AND s.salary_month = MONTH(CURDATE())\n   WHERE ev.grade IN ('S', 'D') AND ev.eval_status = '최종확정' AND ev.eval_year = YEAR(CURDATE())\n   GROUP BY ev.grade;\n"));

        EXAMPLE_POOL.add(new Example(Category.ACCOUNT,
            new Category[]{Category.EMPLOYEE, Category.ACCOUNT, Category.ATTENDANCE},
            new String[]{"관리자","출근","근태","출퇴근"},
            "Q: 관리자 계정의 이번달 출근 현황\nA: SELECT e.emp_name, acc.username, acc.role, a.work_date, a.check_in, a.check_out, a.status\n   FROM account acc\n   JOIN employee e ON acc.emp_id = e.emp_id\n   JOIN attendance a ON e.emp_id = a.emp_id\n   WHERE acc.role = '관리자' AND acc.is_active = 1\n     AND YEAR(a.work_date) = YEAR(CURDATE()) AND MONTH(a.work_date) = MONTH(CURDATE())\n   ORDER BY a.work_date DESC;\n"));

        EXAMPLE_POOL.add(new Example(Category.SALARY,
            new Category[]{Category.EMPLOYEE, Category.SALARY},
            new String[]{"사장","최종승인자","급여","명세"},
            "Q: 사장의 이번달 급여 명세 상세\nA: SELECT e.emp_name, p.position_name,\n          s.base_salary, s.meal_allowance, s.transport_allowance,\n          s.position_allowance, s.overtime_pay, s.gross_salary,\n          s.national_pension, s.health_insurance, s.income_tax,\n          s.unpaid_leave_days, s.unpaid_deduction,\n          s.total_deduction, s.net_salary\n   FROM salary s\n   JOIN employee e ON s.emp_id = e.emp_id\n   JOIN job_position p ON e.position_id = p.position_id\n   WHERE p.position_name = '사장'\n     AND s.salary_year = YEAR(CURDATE()) AND s.salary_month = MONTH(CURDATE());\n"));
    }

    // ================================================================
    // 6. Public API
    // ================================================================
    public static String build(String question) {
        List<Category> matched = pickTopCategories(question);
        List<Example> top = pickTopExamples(question, matched, 2);

        StringBuilder sb = new StringBuilder(4096);
        sb.append(HEADER);
        sb.append("## DDL\n");
        sb.append(SHARED_DDL);

        Set<Category> seen = new LinkedHashSet<>();
        for (Category cat : matched) {
            if (seen.add(cat) && DOMAIN_DDL.containsKey(cat)) sb.append(DOMAIN_DDL.get(cat));
        }
        for (Category cat : matched) {
            if (RULES.containsKey(cat)) sb.append(RULES.get(cat));
        }
        if (!top.isEmpty()) {
            sb.append("## EXAMPLES\n");
            for (Example ex : top) sb.append(ex.qa).append('\n');
        }
        sb.append("## QUESTION\n").append(question).append('\n');
        return sb.toString();
    }
    
    


    /**
     * RAG 서버가 결정한 카테고리 + 예제로 프롬프트 조립 (PromptAssembler 전용).
     *
     * @param categoryNames RAG 서버가 반환한 카테고리 문자열 목록 (예: ["ATTENDANCE","EMPLOYEE"])
     * @param fewShotExamples RAG 서버가 반환한 예제 목록 (null이면 예제 블록 생략)
     * @param question      사용자 원본 질문
     * @return 완성된 프롬프트 문자열
     */
    public static String buildByCategories(
            List<String> categoryNames,
            List<com.hrms.sys.dto.RagContextDTO.FewShotExample> fewShotExamples,
            String question) {

        // 문자열 → Category enum 변환 (매칭 실패 시 무시)
        List<Category> matched = new ArrayList<>();
        for (String name : categoryNames) {
            try {
                matched.add(Category.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("[DynamicPromptBuilder] 알 수 없는 카테고리: " + name);
            }
        }
        if (matched.isEmpty()) matched.add(Category.EMPLOYEE);

        // 기존 build() 로직과 동일한 조립 방식
        StringBuilder sb = new StringBuilder(4096);
        sb.append(HEADER);
        sb.append("## DDL\n");
        sb.append(SHARED_DDL);

        Set<Category> seen = new LinkedHashSet<>();
        for (Category cat : matched) {
            if (seen.add(cat) && DOMAIN_DDL.containsKey(cat)) sb.append(DOMAIN_DDL.get(cat));
        }
        for (Category cat : matched) {
            if (RULES.containsKey(cat)) sb.append(RULES.get(cat));
        }

        // RAG 예제 삽입 (기존 EXAMPLE_POOL 대신 RAG가 선택한 예제 사용)
        if (fewShotExamples != null && !fewShotExamples.isEmpty()) {
            sb.append("## EXAMPLES\n");
            for (com.hrms.sys.dto.RagContextDTO.FewShotExample ex : fewShotExamples) {
                sb.append("Q: ").append(ex.getQuestion()).append("\n");
                sb.append("A: ").append(ex.getSql()).append("\n\n");
            }
        }

        sb.append("## QUESTION\n").append(question).append('\n');
        return sb.toString();
    }

    // ================================================================
    // 7. Multi-category picker
    // ================================================================
    private static final int SECONDARY_THRESHOLD = 2;
    private static final int TERTIARY_THRESHOLD  = 3;

    private static List<Category> pickTopCategories(String question) {
        List<int[]> scored = new ArrayList<>();
        Category[] allCats = Category.values();
        for (int i = 0; i < allCats.length; i++) {
            Map<String, Integer> kwMap = WEIGHTED_KEYWORDS.get(allCats[i]);
            if (kwMap == null) continue;
            int s = 0;
            for (Map.Entry<String, Integer> kw : kwMap.entrySet()) {
                if (question.contains(kw.getKey())) s += kw.getValue();
            }
            scored.add(new int[]{i, s});
        }
        scored.sort((a, b) -> b[1] - a[1]);
        List<Category> result = new ArrayList<>();
        if (scored.isEmpty() || scored.get(0)[1] == 0) { result.add(Category.EMPLOYEE); return result; }
        result.add(allCats[scored.get(0)[0]]);
        if (scored.size() > 1 && scored.get(1)[1] >= SECONDARY_THRESHOLD) {
            Category sec = allCats[scored.get(1)[0]];
            if (sec != result.get(0)) result.add(sec);
        }
        if (scored.size() > 2 && scored.get(2)[1] >= TERTIARY_THRESHOLD) {
            Category ter = allCats[scored.get(2)[0]];
            if (!result.contains(ter)) result.add(ter);
        }
        if (result.size() > 1 && !result.contains(Category.EMPLOYEE)) result.add(0, Category.EMPLOYEE);
        return result;
    }

    private static Category pickBestCategory(String question) { return pickTopCategories(question).get(0); }

    // ================================================================
    // 8. Dynamic example selector
    // ================================================================
    private static List<Example> pickTopExamples(String question, List<Category> matchedCats, int topN) {
        boolean isMulti = matchedCats.size() > 1;
        Set<Category> catSet = new HashSet<>(matchedCats);
        List<int[]> scored = new ArrayList<>();
        for (int i = 0; i < EXAMPLE_POOL.size(); i++) {
            Example ex = EXAMPLE_POOL.get(i);
            boolean belongs = false;
            if (ex.isCrossDomain) { for (Category c : ex.categories) if (catSet.contains(c)) { belongs = true; break; } }
            else belongs = catSet.contains(ex.primaryCategory);
            if (!belongs) continue;
            int s = 0;
            for (String tag : ex.tags) if (question.contains(tag)) s += 2;
            if (isMulti && ex.isCrossDomain) {
                int overlap = 0;
                for (Category c : ex.categories) if (catSet.contains(c)) overlap++;
                s += overlap * 3;
            }
            scored.add(new int[]{i, s});
        }
        if (scored.isEmpty()) return Collections.emptyList();
        scored.sort((a, b) -> b[1] - a[1]);
        List<Example> result = new ArrayList<>(topN);
        for (int j = 0; j < scored.size() && result.size() < topN; j++) {
            int[] pair = scored.get(j);
            if (pair[1] > 0 || result.isEmpty()) result.add(EXAMPLE_POOL.get(pair[0]));
        }
        return result;
    }

    // ================================================================
    // 9. Debug
    // ================================================================
    public static String getMatchedCategory(String question) { return pickBestCategory(question).name(); }

    public static String getMatchedCategories(String question) {
        List<Category> cats = pickTopCategories(question);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cats.size(); i++) { if (i > 0) sb.append(", "); sb.append(cats.get(i).name()); }
        return sb.toString();
    }

    public static int estimateTokens(String prompt) { return (int) (prompt.length() / 2.5); }
}