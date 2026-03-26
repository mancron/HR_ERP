-- =============================================
-- HR ERP Database Schema v4
-- Database  : MySQL 8.0+
-- 문자셋    : utf8mb4 / utf8mb4_unicode_ci
-- 총 테이블 : 16개
-- 수정 이력 :
--   v1  최초 작성
--   v2  audit_log 추가
--   v3  보완 제안 반영 (is_active, eval_status, 생년월일 등)
--   v4  updated_at 전면 제거
--       audit_log 적용 범위 명확화 (핵심 4개 테이블)
--       public_holiday, notification 신규 추가
--       CHECK 제약 추가 (음수 방지, 자기승인 방지 등)
-- =============================================

CREATE DATABASE IF NOT EXISTS hr_erp
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_erp;

-- =============================================
-- 1. 조직 관리
-- =============================================

CREATE TABLE job_position (
    position_id         INT          NOT NULL AUTO_INCREMENT,
    position_name       VARCHAR(20)  NOT NULL                 COMMENT '직급명 (사원/대리/과장/차장/부장)',
    position_level      INT          NOT NULL                 COMMENT '직급 레벨 (1=사원 ~ 5=부장)',
    base_salary         INT          NOT NULL DEFAULT 0       COMMENT '기본급 기준액 (직원 등록 시 자동 세팅 참고값)',
    meal_allowance      INT          NOT NULL DEFAULT 0       COMMENT '식대',
    transport_allowance INT          NOT NULL DEFAULT 0       COMMENT '교통비',
    position_allowance  INT          NOT NULL DEFAULT 0       COMMENT '직책수당',
    is_active           TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성 (폐지 시 FK 참조로 DELETE 불가)',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (position_id)
) COMMENT '직급 테이블';


CREATE TABLE department (
    dept_id        INT         NOT NULL AUTO_INCREMENT,
    dept_name      VARCHAR(50) NOT NULL                 COMMENT '부서명',
    parent_dept_id INT         NULL                     COMMENT '상위 부서 ID (NULL=최상위)',
    manager_id     INT         NULL                     COMMENT '부서장 emp_id',
    dept_level     INT         NOT NULL DEFAULT 1       COMMENT '계층 깊이 (본부=1, 팀=2) — 등록 시 parent.dept_level+1 자동 계산',
    sort_order     INT         NOT NULL DEFAULT 0       COMMENT '동일 레벨 내 UI 출력 순서',
    is_active      TINYINT(1)  NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성',
    closed_at      DATE        NULL                     COMMENT '부서 폐지일',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dept_id),
    FOREIGN KEY (parent_dept_id) REFERENCES department(dept_id) ON DELETE SET NULL
) COMMENT '부서 테이블 (트리 구조)';


-- =============================================
-- 2. 직원 관리
-- =============================================

CREATE TABLE employee (
    emp_id            INT          NOT NULL AUTO_INCREMENT,
    emp_name          VARCHAR(20)  NOT NULL                 COMMENT '직원명',
    emp_no            VARCHAR(20)  NOT NULL                 COMMENT '사번 (EMP001)',
    dept_id           INT          NOT NULL                 COMMENT '소속 부서',
    position_id       INT          NOT NULL                 COMMENT '직급',
    hire_date         DATE         NOT NULL                 COMMENT '입사일',
    resign_date       DATE         NULL                     COMMENT '퇴사일 (NULL=재직 중)',
    emp_type          VARCHAR(10)  NOT NULL DEFAULT '정규직' COMMENT '정규직/계약직/파트타임',
    status            VARCHAR(10)  NOT NULL DEFAULT '재직'   COMMENT '재직/휴직/퇴직',
    base_salary       INT          NOT NULL DEFAULT 0       COMMENT '개인 기본급 (직급 선택 시 기준액 자동 세팅, 수정 가능)',
    birth_date        DATE         NULL                     COMMENT '생년월일',
    gender            CHAR(1)      NULL                     COMMENT '성별 M/F',
    address           VARCHAR(200) NULL                     COMMENT '주소',
    emergency_contact VARCHAR(20)  NULL                     COMMENT '긴급 연락처',
    bank_account      VARCHAR(30)  NULL                     COMMENT '급여 이체 계좌번호',
    email             VARCHAR(100) NULL                     COMMENT '회사 이메일',
    phone             VARCHAR(20)  NULL                     COMMENT '연락처',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (emp_id),
    UNIQUE KEY uk_emp_no (emp_no),
    FOREIGN KEY (dept_id)     REFERENCES department(dept_id),
    FOREIGN KEY (position_id) REFERENCES job_position(position_id),
    CONSTRAINT chk_gender CHECK (gender IN ('M', 'F') OR gender IS NULL)
) COMMENT '직원 테이블 (핵심)';


ALTER TABLE department
    ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES employee(emp_id) ON DELETE SET NULL;


CREATE TABLE personnel_history (
    history_id       INT          NOT NULL AUTO_INCREMENT,
    emp_id           INT          NOT NULL                 COMMENT '대상 직원',
    change_type      VARCHAR(20)  NOT NULL                 COMMENT '발령/승진/전보/퇴직/복직',
    from_dept_id     INT          NULL                     COMMENT '이전 부서',
    to_dept_id       INT          NULL                     COMMENT '발령 부서',
    from_position_id INT          NULL                     COMMENT '이전 직급',
    to_position_id   INT          NULL                     COMMENT '변경 직급',
    change_date      DATE         NOT NULL                 COMMENT '발령 적용일',
    reason           VARCHAR(200) NULL                     COMMENT '발령 사유',
    approved_by      INT          NULL                     COMMENT '승인자 emp_id',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    FOREIGN KEY (emp_id)           REFERENCES employee(emp_id),
    FOREIGN KEY (from_dept_id)     REFERENCES department(dept_id)       ON DELETE SET NULL,
    FOREIGN KEY (to_dept_id)       REFERENCES department(dept_id)       ON DELETE SET NULL,
    FOREIGN KEY (from_position_id) REFERENCES job_position(position_id) ON DELETE SET NULL,
    FOREIGN KEY (to_position_id)   REFERENCES job_position(position_id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by)      REFERENCES employee(emp_id)          ON DELETE SET NULL
) COMMENT '인사발령 이력';


CREATE TABLE account (
    account_id          INT          NOT NULL AUTO_INCREMENT,
    emp_id              INT          NOT NULL                 COMMENT '직원 ID (1인 1계정)',
    username            VARCHAR(50)  NOT NULL                 COMMENT '로그인 ID',
    password_hash       VARCHAR(255) NOT NULL                 COMMENT 'BCrypt 해시',
    role                VARCHAR(20)  NOT NULL DEFAULT '일반'   COMMENT '관리자/HR담당자/일반',
    last_login          DATETIME     NULL                     COMMENT '마지막 로그인 일시',
    is_active           TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성',
    login_attempts      INT          NOT NULL DEFAULT 0       COMMENT '연속 로그인 실패 횟수 — 5회 시 잠금',
    password_changed_at DATETIME     NULL                     COMMENT '마지막 비밀번호 변경 일시',
    locked_at           DATETIME     NULL                     COMMENT '계정 잠금 일시',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_username    (username),
    UNIQUE KEY uk_emp_account (emp_id),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_login_attempts CHECK (login_attempts >= 0)
) COMMENT '로그인 계정';


-- =============================================
-- 3. 근태 관리
-- =============================================

-- 운영 환경: 공공데이터포털 API 배치 적재 권장
-- API: https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo
CREATE TABLE public_holiday (
    holiday_id   INT         NOT NULL AUTO_INCREMENT,
    holiday_date DATE        NOT NULL                 COMMENT '공휴일 날짜',
    holiday_name VARCHAR(50) NOT NULL                 COMMENT '공휴일 명칭',
    holiday_year INT         NOT NULL                 COMMENT '연도',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (holiday_id),
    UNIQUE KEY uk_holiday_date (holiday_date)
) COMMENT '법정 공휴일';


CREATE TABLE attendance (
    att_id         INT          NOT NULL AUTO_INCREMENT,
    emp_id         INT          NOT NULL                 COMMENT '직원 ID',
    work_date      DATE         NOT NULL                 COMMENT '근무일',
    check_in       TIME         NULL                     COMMENT '출근 시간',
    check_out      TIME         NULL                     COMMENT '퇴근 시간',
    work_hours     DECIMAL(4,2) NULL                     COMMENT '실근무시간',
    overtime_hours DECIMAL(4,2) NOT NULL DEFAULT 0       COMMENT '초과근무시간 — overtime_request 승인 후 반영',
    status         VARCHAR(20)  NOT NULL DEFAULT '출근'   COMMENT '출근/지각/결근/휴가/출장',
    note           VARCHAR(200) NULL                     COMMENT '비고',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (att_id),
    UNIQUE KEY uk_emp_date (emp_id, work_date),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_att_status CHECK (status IN ('출근','지각','결근','휴가','출장'))
) COMMENT '출퇴근 기록';


CREATE TABLE leave_request (
    leave_id      INT          NOT NULL AUTO_INCREMENT,
    emp_id        INT          NOT NULL                 COMMENT '신청자',
    leave_type    VARCHAR(20)  NOT NULL                 COMMENT '연차/반차/병가/경조사/공가',
    half_type     VARCHAR(10)  NULL                     COMMENT '반차 구분: 오전/오후',
    start_date    DATE         NOT NULL                 COMMENT '시작일',
    end_date      DATE         NOT NULL                 COMMENT '종료일',
    days          DECIMAL(4,1) NOT NULL                 COMMENT '사용 일수 — 서버에서 공휴일 제외 자동 계산',
    reason        VARCHAR(500) NULL                     COMMENT '사유',
    status        VARCHAR(10)  NOT NULL DEFAULT '대기'   COMMENT '대기/승인/반려/취소',
    approver_id   INT          NULL                     COMMENT '승인자',
    approved_at   DATETIME     NULL                     COMMENT '승인·반려 처리 일시',
    reject_reason VARCHAR(200) NULL                     COMMENT '반려 사유',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leave_id),
    FOREIGN KEY (emp_id)      REFERENCES employee(emp_id),
    FOREIGN KEY (approver_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_self_approve CHECK (approver_id IS NULL OR emp_id != approver_id),
    CONSTRAINT chk_leave_date_order   CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_days         CHECK (days > 0)
) COMMENT '휴가 신청';


CREATE TABLE annual_leave (
    al_id       INT          NOT NULL AUTO_INCREMENT,
    emp_id      INT          NOT NULL                 COMMENT '직원 ID',
    leave_year  INT          NOT NULL                 COMMENT '연도',
    total_days  DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '부여 연차',
    used_days   DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '사용 연차',
    remain_days DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '잔여 연차',
    PRIMARY KEY (al_id),
    UNIQUE KEY uk_emp_year (emp_id, leave_year),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_remain_days CHECK (remain_days >= 0),
    CONSTRAINT chk_used_days   CHECK (used_days <= total_days),
    CONSTRAINT chk_total_days  CHECK (total_days >= 0)
) COMMENT '연차 현황';


CREATE TABLE overtime_request (
    ot_id       INT          NOT NULL AUTO_INCREMENT,
    emp_id      INT          NOT NULL                 COMMENT '신청자',
    ot_date     DATE         NOT NULL                 COMMENT '초과근무 날짜',
    start_time  TIME         NOT NULL                 COMMENT '시작 시간',
    end_time    TIME         NOT NULL                 COMMENT '종료 시간',
    ot_hours    DECIMAL(4,2) NOT NULL                 COMMENT '초과근무 시간',
    reason      VARCHAR(300) NULL                     COMMENT '사유',
    status      VARCHAR(10)  NOT NULL DEFAULT '대기'   COMMENT '대기/승인/반려',
    approver_id INT          NULL                     COMMENT '승인자',
    approved_at DATETIME     NULL                     COMMENT '승인·반려 처리 일시',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ot_id),
    UNIQUE KEY uk_emp_ot_date (emp_id, ot_date),
    FOREIGN KEY (emp_id)      REFERENCES employee(emp_id),
    FOREIGN KEY (approver_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_ot_self_approve CHECK (approver_id IS NULL OR emp_id != approver_id),
    CONSTRAINT chk_ot_time_order   CHECK (end_time > start_time),
    CONSTRAINT chk_ot_hours        CHECK (ot_hours > 0)
) COMMENT '초과근무 신청';


-- =============================================
-- 4. 급여 관리
-- =============================================

-- DECIMAL(5,4) 사용 시 0.03545 -> 0.0355로 잘림 => 반드시 DECIMAL(6,5) 이상 사용
CREATE TABLE deduction_rate (
    rate_id                   INT          NOT NULL AUTO_INCREMENT,
    target_year               INT          NOT NULL COMMENT '적용 연도',
    national_pension_rate     DECIMAL(6,5) NOT NULL COMMENT '국민연금율 (2025: 0.04500)',
    health_insurance_rate     DECIMAL(6,5) NOT NULL COMMENT '건강보험율 (2025: 0.03545)',
    long_term_care_rate       DECIMAL(6,5) NOT NULL COMMENT '장기요양율 — 건강보험료에 곱함 (2025: 0.12950)',
    employment_insurance_rate DECIMAL(6,5) NOT NULL COMMENT '고용보험율 (2025: 0.00900)',
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rate_id),
    UNIQUE KEY uk_target_year (target_year)
) COMMENT '4대보험 공제율 (연도별)';


CREATE TABLE salary (
    salary_id            INT         NOT NULL AUTO_INCREMENT,
    emp_id               INT         NOT NULL                 COMMENT '직원 ID',
    salary_year          INT         NOT NULL                 COMMENT '급여 연도',
    salary_month         INT         NOT NULL                 COMMENT '급여 월 (1~12)',
    base_salary          INT         NOT NULL DEFAULT 0       COMMENT '기본급',
    meal_allowance       INT         NOT NULL DEFAULT 0       COMMENT '식대',
    transport_allowance  INT         NOT NULL DEFAULT 0       COMMENT '교통비',
    position_allowance   INT         NOT NULL DEFAULT 0       COMMENT '직책수당',
    overtime_pay         INT         NOT NULL DEFAULT 0       COMMENT '초과근무수당',
    other_allowance      INT         NOT NULL DEFAULT 0       COMMENT '기타수당',
    gross_salary         INT         NOT NULL DEFAULT 0       COMMENT '지급합계',
    national_pension     INT         NOT NULL DEFAULT 0       COMMENT '국민연금',
    health_insurance     INT         NOT NULL DEFAULT 0       COMMENT '건강보험',
    long_term_care       INT         NOT NULL DEFAULT 0       COMMENT '장기요양보험',
    employment_insurance INT         NOT NULL DEFAULT 0       COMMENT '고용보험',
    income_tax           INT         NOT NULL DEFAULT 0       COMMENT '소득세',
    local_income_tax     INT         NOT NULL DEFAULT 0       COMMENT '지방소득세',
    total_deduction      INT         NOT NULL DEFAULT 0       COMMENT '공제합계',
    net_salary           INT         NOT NULL DEFAULT 0       COMMENT '실수령액',
    pay_date             DATE        NULL                     COMMENT '급여 지급일',
    status               VARCHAR(10) NOT NULL DEFAULT '대기'   COMMENT '대기/완료',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (salary_id),
    UNIQUE KEY uk_emp_salary_month (emp_id, salary_year, salary_month),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_salary_month CHECK (salary_month BETWEEN 1 AND 12),
    CONSTRAINT chk_net_salary   CHECK (net_salary >= 0),
    CONSTRAINT chk_gross_salary CHECK (gross_salary >= 0)
) COMMENT '급여 명세 (월별 스냅샷)';


-- =============================================
-- 5. 인사 평가
-- =============================================

CREATE TABLE evaluation (
    eval_id      INT          NOT NULL AUTO_INCREMENT,
    emp_id       INT          NOT NULL                  COMMENT '평가 대상자',
    eval_year    INT          NOT NULL                  COMMENT '평가 연도',
    eval_period  VARCHAR(10)  NOT NULL                  COMMENT '상반기/하반기/연간',
    eval_type    VARCHAR(20)  NOT NULL DEFAULT '상위평가' COMMENT '자기평가/상위평가/동료평가',
    total_score  DECIMAL(5,2) NULL                      COMMENT '종합 점수 (항목 평균 자동 계산)',
    grade        VARCHAR(5)   NULL                      COMMENT 'S/A/B/C/D',
    eval_comment TEXT         NULL                      COMMENT '평가 코멘트',
    eval_status  VARCHAR(10)  NOT NULL DEFAULT '작성중'  COMMENT '작성중/최종확정 — 최종확정 시에만 급여 연동 허용',
    evaluator_id INT          NULL                      COMMENT '평가자 emp_id',
    confirmed_at DATETIME     NULL                      COMMENT '최종확정 처리 일시',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eval_id),
    UNIQUE KEY uk_emp_eval (emp_id, eval_year, eval_period, eval_type),
    FOREIGN KEY (emp_id)       REFERENCES employee(emp_id),
    FOREIGN KEY (evaluator_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_eval_status CHECK (eval_status IN ('작성중', '최종확정')),
    CONSTRAINT chk_eval_type   CHECK (eval_type   IN ('자기평가', '상위평가', '동료평가')),
    CONSTRAINT chk_eval_period CHECK (eval_period IN ('상반기', '하반기', '연간')),
    CONSTRAINT chk_eval_self
        CHECK (eval_type = '자기평가' OR evaluator_id IS NULL OR emp_id != evaluator_id)
) COMMENT '인사 평가';


CREATE TABLE evaluation_item (
    item_id   INT          NOT NULL AUTO_INCREMENT,
    eval_id   INT          NOT NULL                  COMMENT '평가 ID',
    item_name VARCHAR(50)  NOT NULL                  COMMENT '항목명 (업무성과/직무역량/조직기여도/리더십)',
    score     DECIMAL(5,2) NOT NULL DEFAULT 0        COMMENT '획득 점수',
    max_score DECIMAL(5,2) NOT NULL DEFAULT 100      COMMENT '만점 기준',
    PRIMARY KEY (item_id),
    FOREIGN KEY (eval_id) REFERENCES evaluation(eval_id) ON DELETE CASCADE,
    CONSTRAINT chk_item_score CHECK (score >= 0 AND score <= max_score)
) COMMENT '평가 항목별 점수';


-- =============================================
-- 6. 시스템
-- =============================================

-- 알림은 트랜잭션 외부에서 INSERT — 실패해도 핵심 기능 롤백 없음
CREATE TABLE notification (
    noti_id    BIGINT       NOT NULL AUTO_INCREMENT,
    emp_id     INT          NOT NULL                  COMMENT '수신 직원 ID',
    noti_type  VARCHAR(30)  NOT NULL                  COMMENT 'LEAVE_APPROVED/LEAVE_REJECTED/LEAVE_PENDING/OVERTIME_APPROVED/OVERTIME_PENDING/SALARY_PAID/EVAL_CONFIRMED/PW_CHANGE_REMIND/ACCOUNT_LOCKED',
    ref_table  VARCHAR(50)  NULL                      COMMENT '연관 테이블명',
    ref_id     INT          NULL                      COMMENT '연관 레코드 PK',
    message    VARCHAR(300) NOT NULL                  COMMENT '알림 메시지',
    is_read    TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '0=미읽음, 1=읽음',
    read_at    DATETIME     NULL                      COMMENT '읽은 일시',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (noti_id),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
) COMMENT '알림 이력';


-- 감사 로그 — INSERT 전용, UPDATE/DELETE 금지
-- 적용 대상: employee(base_salary/status/resign_date)
--           account(role/is_active)
--           annual_leave(used_days/remain_days)
--           salary(status)
CREATE TABLE audit_log (
    log_id       BIGINT      NOT NULL AUTO_INCREMENT,
    actor_id     INT         NULL                     COMMENT '작업자 emp_id (시스템 처리 시 NULL)',
    target_table VARCHAR(50) NOT NULL                 COMMENT '변경된 테이블명',
    target_id    INT         NOT NULL                 COMMENT '변경된 레코드 PK',
    action       VARCHAR(10) NOT NULL                 COMMENT 'INSERT/UPDATE/DELETE',
    column_name  VARCHAR(50) NULL                     COMMENT '변경된 컬럼명',
    old_value    TEXT        NULL                     COMMENT '변경 전 값 (민감 정보 마스킹)',
    new_value    TEXT        NULL                     COMMENT '변경 후 값 (민감 정보 마스킹)',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    FOREIGN KEY (actor_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
) COMMENT '감사 로그 — 핵심 4개 테이블 변경 이력';


-- =============================================
-- 7. 인덱스
-- =============================================

CREATE INDEX idx_employee_dept     ON employee(dept_id);
CREATE INDEX idx_employee_status   ON employee(status);
CREATE INDEX idx_employee_position ON employee(position_id);

CREATE INDEX idx_attendance_date   ON attendance(emp_id, work_date);
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_leave_status      ON leave_request(status);
CREATE INDEX idx_leave_emp_date    ON leave_request(emp_id, start_date, end_date);
CREATE INDEX idx_ot_status         ON overtime_request(status);

CREATE INDEX idx_salary_period     ON salary(salary_year, salary_month);
CREATE INDEX idx_salary_emp        ON salary(emp_id);

CREATE INDEX idx_eval_period       ON evaluation(eval_year, eval_period);
CREATE INDEX idx_eval_status       ON evaluation(eval_status);

CREATE INDEX idx_holiday_year      ON public_holiday(holiday_year);

CREATE INDEX idx_noti_emp_read     ON notification(emp_id, is_read);

CREATE INDEX idx_audit_table_id    ON audit_log(target_table, target_id);
CREATE INDEX idx_audit_actor       ON audit_log(actor_id);
CREATE INDEX idx_audit_date        ON audit_log(created_at);


-- =============================================
-- 8. 초기 데이터
-- =============================================

INSERT INTO job_position
    (position_name, position_level, base_salary, meal_allowance, transport_allowance, position_allowance)
VALUES
    ('사원', 1, 2800000, 150000, 100000,      0),
    ('대리', 2, 3300000, 170000, 100000,      0),
    ('과장', 3, 3900000, 200000, 150000, 200000),
    ('차장', 4, 4500000, 200000, 200000, 300000),
    ('부장', 5, 5200000, 200000, 200000, 500000);


INSERT INTO deduction_rate
    (target_year, national_pension_rate, health_insurance_rate, long_term_care_rate, employment_insurance_rate)
VALUES
    (2024, 0.04500, 0.03545, 0.12810, 0.00900),
    (2025, 0.04500, 0.03545, 0.12950, 0.00900);


INSERT INTO department (dept_id, dept_name, parent_dept_id, dept_level, sort_order) VALUES
    (1, '(주)예시회사', NULL, 1, 0),
    (2, '개발본부',     1,    2, 1),
    (3, '경영지원본부', 1,    2, 2),
    (4, '개발1팀',      2,    3, 1),
    (5, '개발2팀',      2,    3, 2),
    (6, '인사팀',       3,    3, 1),
    (7, '재무팀',       3,    3, 2),
    (8, '영업팀',       1,    2, 3);


-- 운영 환경: 공공데이터포털 API 자동 적재 권장
INSERT INTO public_holiday (holiday_date, holiday_name, holiday_year) VALUES
    ('2025-01-01', '신정',       2025),
    ('2025-01-28', '설날 연휴',  2025),
    ('2025-01-29', '설날',       2025),
    ('2025-01-30', '설날 연휴',  2025),
    ('2025-03-01', '3.1절',      2025),
    ('2025-05-05', '어린이날',   2025),
    ('2025-05-06', '대체공휴일', 2025),
    ('2025-06-06', '현충일',     2025),
    ('2025-08-15', '광복절',     2025),
    ('2025-10-03', '개천절',     2025),
    ('2025-10-05', '추석 연휴',  2025),
    ('2025-10-06', '추석',       2025),
    ('2025-10-07', '추석 연휴',  2025),
    ('2025-10-08', '대체공휴일', 2025),
    ('2025-10-09', '한글날',     2025),
    ('2025-12-25', '크리스마스', 2025);


-- =============================================
-- 9. 정합성 확인 쿼리 (운영 점검용)
-- =============================================

-- 계정 없는 재직 직원
SELECT e.emp_no, e.emp_name, '계정 없음' AS issue
FROM employee e
WHERE e.status = '재직'
  AND NOT EXISTS (SELECT 1 FROM account a WHERE a.emp_id = e.emp_id);

-- 연차 음수 발생
SELECT e.emp_name, al.leave_year, al.remain_days
FROM annual_leave al
JOIN employee e ON al.emp_id = e.emp_id
WHERE al.remain_days < 0;

-- 급여 실수령액 불일치
SELECT salary_id, emp_id, salary_year, salary_month,
       net_salary, (gross_salary - total_deduction) AS expected_net
FROM salary
WHERE net_salary != (gross_salary - total_deduction);

-- 퇴직자인데 계정 활성 상태
SELECT e.emp_name, '퇴직자 계정 활성' AS issue
FROM employee e
JOIN account a ON e.emp_id = a.emp_id
WHERE e.status = '퇴직' AND a.is_active = 1;

-- 비활성 직급 사용 중인 재직 직원
SELECT e.emp_name, p.position_name, '폐지된 직급 사용' AS issue
FROM employee e
JOIN job_position p ON e.position_id = p.position_id
WHERE e.status = '재직' AND p.is_active = 0;

-- 비활성 부서 소속 재직 직원
SELECT e.emp_name, d.dept_name, '폐지된 부서 소속' AS issue
FROM employee e
JOIN department d ON e.dept_id = d.dept_id
WHERE e.status = '재직' AND d.is_active = 0;

-- 최종확정 안 된 평가 현황
SELECT e.emp_name, ev.eval_year, ev.eval_period, ev.eval_status
FROM evaluation ev
JOIN employee e ON ev.emp_id = e.emp_id
WHERE ev.eval_status != '최종확정'
  AND ev.eval_year = YEAR(NOW());
