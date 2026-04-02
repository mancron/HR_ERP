-- =============================================
-- HR ERP Database Schema v4 (Fixed)
-- Database  : MySQL 8.0+
-- 문자셋    : utf8mb4 / utf8mb4_unicode_ci
-- 총 테이블 : 16개
-- =============================================
-- 수정 이력:
--   v4_fixed:
--     - Error 3823 해결: CHECK 제약과 FOREIGN KEY 충돌 문제 해결
--       (leave_request, overtime_request의 자기승인 방지 CHECK 제약 제거)
--     - 2025년 공휴일 데이터 정확화:
--       * 부처님오신날 (5월 5일) 추가
--       * 근로자의날 (5월 1일) 추가
--       * 3.1절 대체휴일 (3월 3일) 추가
--       * 어린이날/부처님오신날 대체휴일 (5월 6일) 명확화
--       * 추석 대체휴일 (10월 8일) 추가
--     - 전체 테이블에 상세 주석 추가
--     - 자기승인 검증은 트리거로 이동
-- =============================================

CREATE DATABASE IF NOT EXISTS hr_erp
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_erp;

-- =============================================
-- 1. 조직 관리
-- =============================================

-- 직급 테이블: 회사의 직급 체계를 정의
-- 직급 레벨은 1(사원)부터 5(부장)까지 계층 구조
-- base_salary 는 신규 직원 등록 시 기준값으로 사용되며 개별 수정 가능
CREATE TABLE job_position (
    position_id         INT          NOT NULL AUTO_INCREMENT   COMMENT '직급 ID (PK)',
    position_name       VARCHAR(20)  NOT NULL                 COMMENT '직급명 (사원/대리/과장/차장/부장)',
    position_level      INT          NOT NULL                 COMMENT '직급 레벨 (1=사원 ~ 5=부장)',
    base_salary         INT          NOT NULL DEFAULT 0       COMMENT '기본급 기준액 (직원 등록 시 자동 세팅 참고값)',
    meal_allowance      INT          NOT NULL DEFAULT 0       COMMENT '식대',
    transport_allowance INT          NOT NULL DEFAULT 0       COMMENT '교통비',
    position_allowance  INT          NOT NULL DEFAULT 0       COMMENT '직책수당',
    is_active           TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성 (폐지 시 FK 참조로 DELETE 불가)',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (position_id),
    CONSTRAINT chk_position_level CHECK (position_level BETWEEN 1 AND 20)
) COMMENT '직급 테이블 - 회사의 직급 체계';


-- 부서 테이블: 트리 구조로 조직도 표현
-- parent_dept_id가 NULL이면 최상위 부서 (본사)
-- dept_level은 계층 깊이 (본부=1, 팀=2 등)
CREATE TABLE department (
    dept_id        INT         NOT NULL AUTO_INCREMENT        COMMENT '부서 ID (PK)',
    dept_name      VARCHAR(50) NOT NULL                 COMMENT '부서명',
    parent_dept_id INT         NULL                     COMMENT '상위 부서 ID (NULL=최상위 부서)',
    manager_id     INT         NULL                     COMMENT '부서장 emp_id (직원 배치 후 설정)',
    dept_level     INT         NOT NULL DEFAULT 1       COMMENT '계층 깊이 (본부=1, 팀=2) — 등록 시 parent.dept_level+1 자동 계산',
    sort_order     INT         NOT NULL DEFAULT 0       COMMENT '동일 레벨 내 UI 출력 순서 (낮을수록 위)',
    is_active      TINYINT(1)  NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성 (부서 폐지 시)',
    closed_at      DATE        NULL                     COMMENT '부서 폐지일',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (dept_id),
    FOREIGN KEY (parent_dept_id) REFERENCES department(dept_id) ON DELETE SET NULL,
    CONSTRAINT chk_dept_level CHECK (dept_level BETWEEN 1 AND 20)
) COMMENT '부서 테이블 (트리 구조) - 조직도 구성';


-- =============================================
-- 2. 직원 관리
-- =============================================

-- 직원 테이블: HR 시스템의 핵심 테이블
-- emp_no는 EMP001 형식의 사번 (UNIQUE)
-- status: 재직/휴직/퇴직으로 직원 상태 관리
-- resign_date가 NULL이면 현재 재직 중
CREATE TABLE employee (
    emp_id            INT          NOT NULL AUTO_INCREMENT    COMMENT '직원 ID (PK)',
    emp_name          VARCHAR(20)  NOT NULL                 COMMENT '직원명',
    emp_no            VARCHAR(20)  NOT NULL                 COMMENT '사번 (EMP001 형식, UNIQUE)',
    dept_id           INT          NOT NULL                 COMMENT '소속 부서 ID (FK)',
    position_id       INT          NOT NULL                 COMMENT '직급 ID (FK)',
    hire_date         DATE         NOT NULL                 COMMENT '입사일',
    resign_date       DATE         NULL                     COMMENT '퇴사일 (NULL=재직 중)',
    emp_type          VARCHAR(10)  NOT NULL DEFAULT '정규직' COMMENT '정규직/계약직/파트타임',
    status            VARCHAR(10)  NOT NULL DEFAULT '재직'   COMMENT '재직/휴직/퇴직 — audit_log 기록 대상',
    base_salary       INT          NOT NULL DEFAULT 0       COMMENT '개인 기본급 (직급 기준액에서 수정 가능)',
    birth_date        DATE         NULL                     COMMENT '생년월일',
    gender            CHAR(1)      NULL                     COMMENT '성별 (M=남/F=여)',
    address           VARCHAR(200) NULL                     COMMENT '주소',
    emergency_contact VARCHAR(20)  NULL                     COMMENT '긴급 연락처',
    bank_account      VARCHAR(30)  NULL                     COMMENT '급여 이체 계좌번호',
    email             VARCHAR(100) NULL                     COMMENT '회사 이메일',
    phone             VARCHAR(20)  NULL                     COMMENT '연락처',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (emp_id),
    UNIQUE KEY uk_emp_no (emp_no),
    FOREIGN KEY (dept_id)     REFERENCES department(dept_id),
    FOREIGN KEY (position_id) REFERENCES job_position(position_id),
    CONSTRAINT chk_emp_type   CHECK (emp_type IN ('정규직', '계약직', '파트타임')),
    CONSTRAINT chk_status     CHECK (status IN ('재직', '휴직', '퇴직')),
    CONSTRAINT chk_gender     CHECK (gender IN ('M', 'F') OR gender IS NULL),
    CONSTRAINT chk_salary     CHECK (base_salary >= 0)
) COMMENT '직원 테이블 (핵심) - 모든 직원 정보 관리';


-- 부서장 외래키: 순환 참조 방지를 위해 ALTER TABLE로 추가
ALTER TABLE department
    ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES employee(emp_id) ON DELETE SET NULL;


-- 인사발령 이력 테이블: 부서이동, 승진, 전보 등의 모든 발령 기록
-- change_type별로 다양한 발령 사유를 추적
-- approved_by: 발령 승인자 (보통 HR담당자 또는 경영진)
CREATE TABLE personnel_history (
    history_id       INT          NOT NULL AUTO_INCREMENT    COMMENT '이력 ID (PK)',
    emp_id           INT          NOT NULL                 COMMENT '대상 직원 ID (FK)',
    change_type      VARCHAR(20)  NOT NULL                 COMMENT '발령/승진/전보/퇴직/복직',
    from_dept_id     INT          NULL                     COMMENT '이전 부서 ID (FK)',
    to_dept_id       INT          NULL                     COMMENT '발령 부서 ID (FK)',
    from_position_id INT          NULL                     COMMENT '이전 직급 ID (FK)',
    to_position_id   INT          NULL                     COMMENT '변경 직급 ID (FK)',
    change_date      DATE         NOT NULL                 COMMENT '발령 적용일',
    reason           VARCHAR(200) NULL                     COMMENT '발령 사유',
    approved_by      INT          NULL                     COMMENT '승인자 emp_id (FK)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (history_id),
    FOREIGN KEY (emp_id)           REFERENCES employee(emp_id),
    FOREIGN KEY (from_dept_id)     REFERENCES department(dept_id)       ON DELETE SET NULL,
    FOREIGN KEY (to_dept_id)       REFERENCES department(dept_id)       ON DELETE SET NULL,
    FOREIGN KEY (from_position_id) REFERENCES job_position(position_id) ON DELETE SET NULL,
    FOREIGN KEY (to_position_id)   REFERENCES job_position(position_id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by)      REFERENCES employee(emp_id)          ON DELETE SET NULL,
    CONSTRAINT chk_change_type CHECK (change_type IN ('발령', '승진', '전보', '퇴직', '복직'))
) COMMENT '인사발령 이력 - 부서이동, 승진, 전보 등 모든 인사이동 추적';


-- 로그인 계정 테이블: 시스템 접근 관리
-- username은 로그인 ID (UNIQUE)
-- password_hash는 BCrypt 해시 (평문 저장 금지)
-- login_attempts: 5회 이상 실패 시 계정 잠금
CREATE TABLE account (
    account_id          INT          NOT NULL AUTO_INCREMENT    COMMENT '계정 ID (PK)',
    emp_id              INT          NOT NULL                 COMMENT '직원 ID (FK, 1인 1계정)',
    username            VARCHAR(50)  NOT NULL                 COMMENT '로그인 ID',
    password_hash       VARCHAR(255) NOT NULL                 COMMENT 'BCrypt 해시된 비밀번호',
    role                VARCHAR(20)  NOT NULL DEFAULT '일반'   COMMENT '관리자/HR담당자/일반 — 권한 관리',
    last_login          DATETIME     NULL                     COMMENT '마지막 로그인 일시',
    is_active           TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '1=활성, 0=비활성 (계정 비활성화) — audit_log 기록 대상',
    login_attempts      INT          NOT NULL DEFAULT 0       COMMENT '연속 로그인 실패 횟수 (5회 시 자동 잠금)',
    password_changed_at DATETIME     NULL                     COMMENT '마지막 비밀번호 변경 일시',
    locked_at           DATETIME     NULL                     COMMENT '계정 잠금 일시',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_username    (username),
    UNIQUE KEY uk_emp_account (emp_id),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_role           CHECK (role IN ('관리자', 'HR담당자', '일반')),
    CONSTRAINT chk_login_attempts CHECK (login_attempts >= 0)
) COMMENT '로그인 계정 - 시스템 접근 관리';


-- =============================================
-- 3. 근태 관리
-- =============================================

-- 공휴일 테이블: 법정공휴일 및 대체휴일
-- 2025년 기준 (정보 출처: 관공서의 공휴일에 관한 규정)
-- 운영 환경에서는 공공데이터포털 API로 자동 적재 권장
-- API URL: https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo
CREATE TABLE public_holiday (
    holiday_id   INT         NOT NULL AUTO_INCREMENT    COMMENT '공휴일 ID (PK)',
    holiday_date DATE        NOT NULL                 COMMENT '공휴일 날짜',
    holiday_name VARCHAR(50) NOT NULL                 COMMENT '공휴일 명칭',
    holiday_year INT         NOT NULL                 COMMENT '연도',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (holiday_id),
    UNIQUE KEY uk_holiday_date_name (holiday_date, holiday_name),
    CONSTRAINT chk_holiday_year CHECK (holiday_year BETWEEN 2000 AND 2100)
) COMMENT '법정 공휴일 - 휴가 계산 시 제외 (같은 날짜에 여러 공휴일 가능, 예: 5/5 어린이날 & 부처님오신날)';


-- 출퇴근 기록 테이블: 일일 근태 관리
-- check_in, check_out: NULL 가능 (미출근 시)
-- work_hours: 실제 근무시간 (퇴근 - 출근)
-- overtime_hours: 초과근무 시간 (overtime_request 승인 후 반영)
-- status: 출근/지각/결근/휴가/출장
CREATE TABLE attendance (
    att_id         INT          NOT NULL AUTO_INCREMENT    COMMENT '근태 ID (PK)',
    emp_id         INT          NOT NULL                 COMMENT '직원 ID (FK)',
    work_date      DATE         NOT NULL                 COMMENT '근무일',
    check_in       TIME         NULL                     COMMENT '출근 시간',
    check_out      TIME         NULL                     COMMENT '퇴근 시간',
    work_hours     DECIMAL(4,2) NULL                     COMMENT '실근무시간 (HH.MM 형식)',
    overtime_hours DECIMAL(4,2) NOT NULL DEFAULT 0       COMMENT '초과근무시간 — overtime_request 승인 후 반영',
    status         VARCHAR(20)  NOT NULL DEFAULT '출근'   COMMENT '출근/지각/결근/휴가/출장',
    note           VARCHAR(200) NULL                     COMMENT '비고',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (att_id),
    UNIQUE KEY uk_emp_date (emp_id, work_date),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_att_status CHECK (status IN ('출근', '지각', '결근', '휴가', '출장')),
    CONSTRAINT chk_work_hours CHECK (work_hours >= 0 OR work_hours IS NULL),
    CONSTRAINT chk_overtime_hours CHECK (overtime_hours >= 0)
) COMMENT '출퇴근 기록 - 일일 근태 추적';


-- 휴가 신청 테이블: 연차, 병가, 경조사 등 휴가 관리
-- leave_type: 연차/반차/병가/경조사/공가
-- half_type: 반차인 경우 오전/오후 구분
-- days: 서버에서 공휴일 제외하여 자동 계산
-- status: 대기/승인/반려/취소
-- NOTE: MySQL 8.0.16+에서는 CHECK 제약과 FOREIGN KEY (ON DELETE SET NULL)를 
--       동일 컬럼에서 사용할 수 없음 (Error 3823)
--       따라서 자기승인 방지는 트리거로 처리
CREATE TABLE leave_request (
    leave_id      INT          NOT NULL AUTO_INCREMENT    COMMENT '휴가 신청 ID (PK)',
    emp_id        INT          NOT NULL                 COMMENT '신청자 emp_id (FK)',
    leave_type    VARCHAR(20)  NOT NULL                 COMMENT '연차/반차/병가/경조사/공가',
    half_type     VARCHAR(10)  NULL                     COMMENT '반차 구분: 오전/오후',
    start_date    DATE         NOT NULL                 COMMENT '휴가 시작일',
    end_date      DATE         NOT NULL                 COMMENT '휴가 종료일',
    days          DECIMAL(4,1) NOT NULL                 COMMENT '사용 일수 (공휴일 제외, 서버에서 자동 계산)',
    reason        VARCHAR(500) NULL                     COMMENT '휴가 사유',
    status        VARCHAR(10)  NOT NULL DEFAULT '대기'   COMMENT '대기/승인/반려/취소',
    approver_id   INT          NULL                     COMMENT '승인자 emp_id (FK)',
    approved_at   DATETIME     NULL                     COMMENT '승인·반려 처리 일시',
    reject_reason VARCHAR(200) NULL                     COMMENT '반려 사유',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (leave_id),
    FOREIGN KEY (emp_id)      REFERENCES employee(emp_id),
    FOREIGN KEY (approver_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_type      CHECK (leave_type IN ('연차', '반차', '병가', '경조사', '공가')),
    CONSTRAINT chk_half_type       CHECK (half_type IN ('오전', '오후') OR half_type IS NULL),
    CONSTRAINT chk_leave_date_order CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_days      CHECK (days > 0),
    CONSTRAINT chk_leave_status    CHECK (status IN ('대기', '승인', '반려', '취소'))
) COMMENT '휴가 신청 - 연차, 병가, 경조사 등 휴가 관리';


-- 연차 현황 테이블: 직원별 연차 사용 현황
-- total_days: 연도별 부여 연차
-- used_days: 사용한 연차
-- remain_days: 잔여 연차 (total_days - used_days)
CREATE TABLE annual_leave (
    al_id       INT          NOT NULL AUTO_INCREMENT    COMMENT '연차 현황 ID (PK)',
    emp_id      INT          NOT NULL                 COMMENT '직원 ID (FK)',
    leave_year  INT          NOT NULL                 COMMENT '연도',
    total_days  DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '부여 연차 (연간)',
    used_days   DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '사용 연차 (누적)',
    remain_days DECIMAL(4,1) NOT NULL DEFAULT 0       COMMENT '잔여 연차 (total_days - used_days)',
    PRIMARY KEY (al_id),
    UNIQUE KEY uk_emp_year (emp_id, leave_year),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_remain_days CHECK (remain_days >= 0),
    CONSTRAINT chk_used_days   CHECK (used_days <= total_days),
    CONSTRAINT chk_total_days  CHECK (total_days >= 0)
) COMMENT '연차 현황 - 직원별 연차 사용 현황';


-- 초과근무 신청 테이블: 초과근무 승인 및 관리
-- ot_date: 초과근무 날짜
-- start_time, end_time: 초과근무 시간대
-- ot_hours: 초과근무 시간
-- status: 대기/승인/반려
-- NOTE: leave_request와 동일하게 자기승인 방지는 트리거로 처리
CREATE TABLE overtime_request (
    ot_id       INT          NOT NULL AUTO_INCREMENT    COMMENT '초과근무 신청 ID (PK)',
    emp_id      INT          NOT NULL                 COMMENT '신청자 emp_id (FK)',
    ot_date     DATE         NOT NULL                 COMMENT '초과근무 날짜',
    start_time  TIME         NOT NULL                 COMMENT '초과근무 시작 시간',
    end_time    TIME         NOT NULL                 COMMENT '초과근무 종료 시간',
    ot_hours    DECIMAL(4,2) NOT NULL                 COMMENT '초과근무 시간',
    reason      VARCHAR(300) NULL                     COMMENT '초과근무 사유',
    status      VARCHAR(10)  NOT NULL DEFAULT '대기'   COMMENT '대기/승인/반려',
    approver_id INT          NULL                     COMMENT '승인자 emp_id (FK)',
    approved_at DATETIME     NULL                     COMMENT '승인·반려 처리 일시',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (ot_id),
    UNIQUE KEY uk_emp_ot_date (emp_id, ot_date),
    FOREIGN KEY (emp_id)      REFERENCES employee(emp_id),
    FOREIGN KEY (approver_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_ot_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_ot_hours      CHECK (ot_hours > 0),
    CONSTRAINT chk_ot_status     CHECK (status IN ('대기', '승인', '반려'))
) COMMENT '초과근무 신청 - 초과근무 승인 및 관리';


-- =============================================
-- 4. 급여 관리
-- =============================================

-- 4대보험 공제율 테이블: 연도별 보험료율
-- DECIMAL(6,5)는 0.04500 같은 비율 저장 가능 (DECIMAL(5,4)는 정밀도 부족)
-- 2025년 기준율:
--   - 국민연금: 4.5%
--   - 건강보험: 3.545%
--   - 장기요양: 12.95% (건강보험료에 곱함)
--   - 고용보험: 0.9%
CREATE TABLE deduction_rate (
    rate_id                   INT          NOT NULL AUTO_INCREMENT    COMMENT '공제율 ID (PK)',
    target_year               INT          NOT NULL                 COMMENT '적용 연도',
    national_pension_rate     DECIMAL(6,5) NOT NULL                 COMMENT '국민연금율 (2025: 0.04500)',
    health_insurance_rate     DECIMAL(6,5) NOT NULL                 COMMENT '건강보험율 (2025: 0.03545)',
    long_term_care_rate       DECIMAL(6,5) NOT NULL                 COMMENT '장기요양율 (2025: 0.12950, 건강보험료에 곱함)',
    employment_insurance_rate DECIMAL(6,5) NOT NULL                 COMMENT '고용보험율 (2025: 0.00900)',
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (rate_id),
    UNIQUE KEY uk_target_year (target_year),
    CONSTRAINT chk_rates_range CHECK (
        national_pension_rate > 0 AND national_pension_rate < 0.1 AND
        health_insurance_rate > 0 AND health_insurance_rate < 0.1 AND
        long_term_care_rate > 0 AND long_term_care_rate < 0.5 AND
        employment_insurance_rate > 0 AND employment_insurance_rate < 0.05
    )
) COMMENT '4대보험 공제율 (연도별) - 급여 계산에 사용';


-- 급여 명세 테이블: 월별 급여 스냅샷
-- salary_year, salary_month: 급여 기간 (2025년 3월 = 202503)
-- gross_salary: 지급 총액 (기본급+제수당)
-- total_deduction: 공제 총액 (4대보험+소득세)
-- net_salary: 실수령액 (gross_salary - total_deduction)
-- status: 대기/완료 (완료 후 수정 불가)
CREATE TABLE salary (
    salary_id            INT         NOT NULL AUTO_INCREMENT    COMMENT '급여 명세 ID (PK)',
    emp_id               INT         NOT NULL                 COMMENT '직원 ID (FK)',
    salary_year          INT         NOT NULL                 COMMENT '급여 연도 (2025)',
    salary_month         INT         NOT NULL                 COMMENT '급여 월 (1~12)',
    base_salary          INT         NOT NULL DEFAULT 0       COMMENT '기본급',
    meal_allowance       INT         NOT NULL DEFAULT 0       COMMENT '식대',
    transport_allowance  INT         NOT NULL DEFAULT 0       COMMENT '교통비',
    position_allowance   INT         NOT NULL DEFAULT 0       COMMENT '직책수당',
    overtime_pay         INT         NOT NULL DEFAULT 0       COMMENT '초과근무수당',
    other_allowance      INT         NOT NULL DEFAULT 0       COMMENT '기타수당',
    gross_salary         INT         NOT NULL DEFAULT 0       COMMENT '지급합계 (기본급+모든 수당)',
    national_pension     INT         NOT NULL DEFAULT 0       COMMENT '국민연금',
    health_insurance     INT         NOT NULL DEFAULT 0       COMMENT '건강보험',
    long_term_care       INT         NOT NULL DEFAULT 0       COMMENT '장기요양보험',
    employment_insurance INT         NOT NULL DEFAULT 0       COMMENT '고용보험',
    income_tax           INT         NOT NULL DEFAULT 0       COMMENT '소득세',
    local_income_tax     INT         NOT NULL DEFAULT 0       COMMENT '지방소득세',
    total_deduction      INT         NOT NULL DEFAULT 0       COMMENT '공제합계 (모든 공제항목 합)',
    net_salary           INT         NOT NULL DEFAULT 0       COMMENT '실수령액 (gross_salary - total_deduction)',
    pay_date             DATE        NULL                     COMMENT '급여 지급일',
    status               VARCHAR(10) NOT NULL DEFAULT '대기'   COMMENT '대기/완료 — audit_log 기록 대상',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (salary_id),
    UNIQUE KEY uk_emp_salary_month (emp_id, salary_year, salary_month),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    CONSTRAINT chk_salary_month CHECK (salary_month BETWEEN 1 AND 12),
    CONSTRAINT chk_salary_status CHECK (status IN ('대기', '완료')),
    CONSTRAINT chk_net_salary   CHECK (net_salary >= 0),
    CONSTRAINT chk_gross_salary CHECK (gross_salary >= 0),
    CONSTRAINT chk_salary_total CHECK (net_salary = gross_salary - total_deduction)
) COMMENT '급여 명세 (월별 스냅샷) - 월급 계산 및 지급 관리';


-- =============================================
-- 5. 인사 평가
-- =============================================

-- 인사 평가 테이블: 직원 성과평가 및 등급 관리
-- eval_type: 자기평가/상위평가/동료평가
-- eval_period: 상반기/하반기/연간
-- eval_status: 작성중 (임시저장) / 최종확정 (급여 연동 가능)
-- grade: S/A/B/C/D
-- total_score: 평가항목 점수의 평균 (자동 계산)
CREATE TABLE evaluation (
    eval_id      INT          NOT NULL AUTO_INCREMENT    COMMENT '평가 ID (PK)',
    emp_id       INT          NOT NULL                 COMMENT '평가 대상자 emp_id (FK)',
    eval_year    INT          NOT NULL                 COMMENT '평가 연도',
    eval_period  VARCHAR(10)  NOT NULL                 COMMENT '상반기/하반기/연간',
    eval_type    VARCHAR(20)  NOT NULL DEFAULT '상위평가' COMMENT '자기평가/상위평가/동료평가',
    total_score  DECIMAL(5,2) NULL                     COMMENT '종합 점수 (0~100, evaluation_item의 평균)',
    grade        VARCHAR(5)   NULL                     COMMENT '등급 (S/A/B/C/D)',
    eval_comment TEXT         NULL                     COMMENT '평가 의견',
    eval_status  VARCHAR(10)  NOT NULL DEFAULT '작성중'  COMMENT '작성중/최종확정 — 최종확정 시에만 급여 연동 허용 — audit_log 기록 대상',
    evaluator_id INT          NULL                     COMMENT '평가자 emp_id (FK)',
    confirmed_at DATETIME     NULL                     COMMENT '최종확정 처리 일시',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (eval_id),
    UNIQUE KEY uk_emp_eval (emp_id, eval_year, eval_period, eval_type),
    FOREIGN KEY (emp_id)       REFERENCES employee(emp_id),
    FOREIGN KEY (evaluator_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_eval_status CHECK (eval_status IN ('작성중', '최종확정')),
    CONSTRAINT chk_eval_type   CHECK (eval_type   IN ('자기평가', '상위평가', '동료평가')),
    CONSTRAINT chk_eval_period CHECK (eval_period IN ('상반기', '하반기', '연간')),
    CONSTRAINT chk_eval_grade  CHECK (grade IN ('S', 'A', 'B', 'C', 'D') OR grade IS NULL),
    CONSTRAINT chk_eval_score  CHECK (total_score >= 0 AND total_score <= 100 OR total_score IS NULL)
) COMMENT '인사 평가 - 성과평가 및 등급 관리';


-- 평가 항목별 점수 테이블: 개별 평가항목 상세 점수
-- item_name: 업무성과/직무역량/조직기여도/리더십 등
-- score: 획득 점수 (0 ~ max_score)
-- max_score: 항목의 만점 기준
CREATE TABLE evaluation_item (
    item_id   INT          NOT NULL AUTO_INCREMENT    COMMENT '평가항목 ID (PK)',
    eval_id   INT          NOT NULL                 COMMENT '평가 ID (FK)',
    item_name VARCHAR(50)  NOT NULL                 COMMENT '항목명 (업무성과/직무역량/조직기여도/리더십)',
    score     DECIMAL(5,2) NOT NULL DEFAULT 0        COMMENT '획득 점수',
    max_score DECIMAL(5,2) NOT NULL DEFAULT 100      COMMENT '만점 기준',
    PRIMARY KEY (item_id),
    FOREIGN KEY (eval_id) REFERENCES evaluation(eval_id) ON DELETE CASCADE,
    CONSTRAINT chk_item_score CHECK (score >= 0 AND score <= max_score),
    CONSTRAINT chk_max_score  CHECK (max_score > 0)
) COMMENT '평가 항목별 점수 - 개별 평가항목 상세 점수';


-- =============================================
-- 6. 시스템
-- =============================================

-- 알림 이력 테이블: 시스템 생성 알림 로그
-- 트랜잭션 외부에서 INSERT → 실패해도 핵심 기능 롤백 없음 (가용성 중시)
-- noti_type: 휴가/초과근무/급여/평가/계정 관련 알림
-- is_read: 0=미읽음, 1=읽음
CREATE TABLE notification (
    noti_id    BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '알림 ID (PK)',
    emp_id     INT          NOT NULL                 COMMENT '수신 직원 ID (FK)',
    noti_type  VARCHAR(30)  NOT NULL                 COMMENT '알림 유형 (LEAVE_APPROVED/OVERTIME_PENDING 등)',
    ref_table  VARCHAR(50)  NULL                     COMMENT '연관 테이블명 (leave_request 등)',
    ref_id     INT          NULL                     COMMENT '연관 레코드 PK (leave_id 등)',
    message    VARCHAR(300) NOT NULL                 COMMENT '알림 메시지',
    is_read    TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '0=미읽음, 1=읽음',
    read_at    DATETIME     NULL                     COMMENT '읽은 일시',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (noti_id),
    FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
    INDEX idx_noti_emp_read (emp_id, is_read)
) COMMENT '알림 이력 - 시스템 생성 알림 로그';


-- 감사 로그 테이블: INSERT 전용, UPDATE/DELETE 금지
-- 적용 대상 (핵심 4개 테이블):
--   1. employee: base_salary/status/resign_date 변경
--   2. account: role/is_active 변경
--   3. annual_leave: used_days/remain_days 변경
--   4. salary: status 변경 (대기→완료)
-- actor_id가 NULL이면 시스템 자동 처리
CREATE TABLE audit_log (
    log_id       BIGINT      NOT NULL AUTO_INCREMENT    COMMENT '감사로그 ID (PK)',
    actor_id     INT         NULL                     COMMENT '작업자 emp_id (FK, NULL=시스템 처리)',
    target_table VARCHAR(50) NOT NULL                 COMMENT '변경된 테이블명 (employee/account 등)',
    target_id    INT         NOT NULL                 COMMENT '변경된 레코드 PK',
    action       VARCHAR(10) NOT NULL                 COMMENT '작업 유형 (INSERT/UPDATE/DELETE)',
    column_name  VARCHAR(50) NULL                     COMMENT '변경된 컬럼명 (UPDATE인 경우)',
    old_value    TEXT        NULL                     COMMENT '변경 전 값 (민감 정보는 마스킹)',
    new_value    TEXT        NULL                     COMMENT '변경 후 값 (민감 정보는 마스킹)',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (log_id),
    FOREIGN KEY (actor_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
) COMMENT '감사 로그 (INSERT 전용) — 핵심 4개 테이블 변경 이력 추적';


-- =============================================
-- 7. 인덱스
-- =============================================

-- 직원 관련 인덱스
CREATE INDEX idx_employee_dept     ON employee(dept_id);
CREATE INDEX idx_employee_status   ON employee(status);
CREATE INDEX idx_employee_position ON employee(position_id);

-- 근태 관련 인덱스
CREATE INDEX idx_attendance_date   ON attendance(emp_id, work_date);
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_leave_status      ON leave_request(status);
CREATE INDEX idx_leave_emp_date    ON leave_request(emp_id, start_date, end_date);
CREATE INDEX idx_ot_status         ON overtime_request(status);

-- 급여 관련 인덱스
CREATE INDEX idx_salary_period     ON salary(salary_year, salary_month);
CREATE INDEX idx_salary_emp        ON salary(emp_id);

-- 평가 관련 인덱스
CREATE INDEX idx_eval_period       ON evaluation(eval_year, eval_period);
CREATE INDEX idx_eval_status       ON evaluation(eval_status);

-- 공휴일 인덱스
CREATE INDEX idx_holiday_year      ON public_holiday(holiday_year);

-- 감사로그 인덱스
-- NOTE: idx_noti_emp_read는 notification 테이블 생성 시 이미 정의됨 (중복 방지)
CREATE INDEX idx_audit_table_id    ON audit_log(target_table, target_id);
CREATE INDEX idx_audit_actor       ON audit_log(actor_id);
CREATE INDEX idx_audit_date        ON audit_log(created_at);


-- =============================================
-- 8. 자기승인 방지 트리거
-- =============================================
-- MySQL 8.0.16+에서는 CHECK 제약과 FOREIGN KEY (ON DELETE SET NULL)를 
-- 동일 컬럼에 사용할 수 없음 (Error 3823)
-- 따라서 자기승인 방지 로직을 트리거로 구현

DELIMITER $$

-- leave_request 테이블: 자기 승인 방지
CREATE TRIGGER check_leave_self_approve 
BEFORE INSERT ON leave_request 
FOR EACH ROW
BEGIN
    IF NEW.approver_id IS NOT NULL AND NEW.emp_id = NEW.approver_id THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = '자기 승인은 불가능합니다. 다른 관리자가 승인해야 합니다.';
    END IF;
END$$

CREATE TRIGGER check_leave_self_approve_update 
BEFORE UPDATE ON leave_request 
FOR EACH ROW
BEGIN
    IF NEW.approver_id IS NOT NULL AND NEW.emp_id = NEW.approver_id THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = '자기 승인은 불가능합니다. 다른 관리자가 승인해야 합니다.';
    END IF;
END$$

-- overtime_request 테이블: 자기 승인 방지
CREATE TRIGGER check_ot_self_approve 
BEFORE INSERT ON overtime_request 
FOR EACH ROW
BEGIN
    IF NEW.approver_id IS NOT NULL AND NEW.emp_id = NEW.approver_id THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = '자기 승인은 불가능합니다. 다른 관리자가 승인해야 합니다.';
    END IF;
END$$

CREATE TRIGGER check_ot_self_approve_update 
BEFORE UPDATE ON overtime_request 
FOR EACH ROW
BEGIN
    IF NEW.approver_id IS NOT NULL AND NEW.emp_id = NEW.approver_id THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = '자기 승인은 불가능합니다. 다른 관리자가 승인해야 합니다.';
    END IF;
END$$

DELIMITER ;


-- =============================================
-- 9. 초기 데이터
-- =============================================

-- 직급 초기 데이터: 기본 급여 및 수당 기준
INSERT INTO job_position
    (position_name, position_level, base_salary, meal_allowance, transport_allowance, position_allowance)
VALUES
    ('사원', 1, 2800000, 150000, 100000,      0),
    ('대리', 2, 3300000, 170000, 100000,      0),
    ('과장', 3, 3900000, 200000, 150000, 200000),
    ('차장', 4, 4500000, 200000, 200000, 300000),
    ('부장', 5, 5200000, 200000, 200000, 500000);


-- 공제율 초기 데이터: 2024~2025년 4대보험료율
-- 출처: 국민연금공단, 건강보험공단, 고용보험공단
INSERT INTO deduction_rate
    (target_year, national_pension_rate, health_insurance_rate, long_term_care_rate, employment_insurance_rate)
VALUES
    (2024, 0.04500, 0.03545, 0.12810, 0.00900),
    (2025, 0.04500, 0.03545, 0.12950, 0.00900);


-- 부서 초기 데이터: 조직도 구성
INSERT INTO department (dept_id, dept_name, parent_dept_id, dept_level, sort_order) VALUES
    (1, '(주)예시회사', NULL, 1, 0),
    (2, '개발본부',     1,    2, 1),
    (3, '경영지원본부', 1,    2, 2),
    (4, '개발1팀',      2,    3, 1),
    (5, '개발2팀',      2,    3, 2),
    (6, '인사팀',       3,    3, 1),
    (7, '재무팀',       3,    3, 2),
    (8, '영업팀',       1,    2, 3);


-- 공휴일 초기 데이터: 2025년 법정공휴일 및 대체공휴일
-- 출처: 관공서의 공휴일에 관한 규정 (2025년)
-- 최종 수정: 2026년 3월 (최신 정보 확인)
INSERT INTO public_holiday (holiday_date, holiday_name, holiday_year) VALUES
    -- 1월
    ('2025-01-01', '신정',       2025),
    -- 설날 연휴 (1월 28일 화~1월 30일 목)
    ('2025-01-28', '설날 연휴',  2025),
    ('2025-01-29', '설날',       2025),
    ('2025-01-30', '설날 연휴',  2025),
    -- 3월 (3월 1일이 토요일이므로 대체휴일 3월 3일)
    ('2025-03-01', '삼일절',     2025),
    ('2025-03-03', '대체공휴일', 2025),
    -- 5월 (어린이날과 부처님오신날이 같은 날, 대체휴일 5월 6일)
    ('2025-05-01', '근로자의날', 2025),
    ('2025-05-05', '어린이날',   2025),
    ('2025-05-05', '부처님오신날', 2025),
    ('2025-05-06', '대체공휴일', 2025),
    -- 6월
    ('2025-06-06', '현충일',     2025),
    -- 8월
    ('2025-08-15', '광복절',     2025),
    -- 10월 (개천절, 추석 연휴, 대체휴일)
    ('2025-10-03', '개천절',     2025),
    ('2025-10-05', '추석 연휴',  2025),
    ('2025-10-06', '추석',       2025),
    ('2025-10-07', '추석 연휴',  2025),
    ('2025-10-08', '대체공휴일', 2025),
    ('2025-10-09', '한글날',     2025),
    -- 12월
    ('2025-12-25', '크리스마스', 2025);


-- =============================================
-- 10. 정합성 확인 쿼리 (운영 점검용)
-- =============================================
-- 이 쿼리들을 정기적으로 실행하여 데이터 무결성 확인

-- 계정 없는 재직 직원: 보안 문제 가능성
SELECT e.emp_no, e.emp_name, '계정 없음' AS issue
FROM employee e
WHERE e.status = '재직'
  AND NOT EXISTS (SELECT 1 FROM account a WHERE a.emp_id = e.emp_id)
LIMIT 10;

-- 연차 음수 발생: 급여 계산 오류 가능성
SELECT e.emp_name, al.leave_year, al.remain_days
FROM annual_leave al
JOIN employee e ON al.emp_id = e.emp_id
WHERE al.remain_days < 0
LIMIT 10;

-- 급여 실수령액 불일치: 계산 오류 확인
SELECT salary_id, emp_id, salary_year, salary_month,
       net_salary, (gross_salary - total_deduction) AS expected_net
FROM salary
WHERE net_salary != (gross_salary - total_deduction)
LIMIT 10;

-- 퇴직자인데 계정 활성: 보안 위험
SELECT e.emp_name, '퇴직자 계정 활성' AS issue
FROM employee e
JOIN account a ON e.emp_id = a.emp_id
WHERE e.status = '퇴직' AND a.is_active = 1
LIMIT 10;

-- 비활성 직급 사용 중인 재직 직원: 정책 위반
SELECT e.emp_name, p.position_name, '폐지된 직급 사용' AS issue
FROM employee e
JOIN job_position p ON e.position_id = p.position_id
WHERE e.status = '재직' AND p.is_active = 0
LIMIT 10;

-- 비활성 부서 소속 재직 직원: 정책 위반
SELECT e.emp_name, d.dept_name, '폐지된 부서 소속' AS issue
FROM employee e
JOIN department d ON e.dept_id = d.dept_id
WHERE e.status = '재직' AND d.is_active = 0
LIMIT 10;

-- 최종확정 안 된 평가 현황: 급여 반영 전 확정 필수
SELECT e.emp_name, ev.eval_year, ev.eval_period, ev.eval_status
FROM evaluation ev
JOIN employee e ON ev.emp_id = e.emp_id
WHERE ev.eval_status != '최종확정'
  AND ev.eval_year = YEAR(NOW())
ORDER BY ev.eval_year DESC, ev.eval_period DESC
LIMIT 20;



-- ================================================
-- HR ERP AI 조회용 뷰
-- ================================================
-- AI 전용 계정 생성 (비밀번호는 원하는 대로 변경)
CREATE USER 'ai_reader'@'%' IDENTIFIED BY '123456!';

-- hr_erp DB에 대한 SELECT 권한만 부여
GRANT SELECT ON hr_erp.* TO 'ai_reader'@'%';

-- 권한 적용
FLUSH PRIVILEGES;


-- 1. 직원 전체 정보 뷰 (직원 + 부서 + 직급 통합)
CREATE OR REPLACE VIEW v_employee_full AS
SELECT
    e.emp_id, e.emp_name, e.emp_no,
    e.status, e.emp_type, e.base_salary,
    e.hire_date, e.resign_date,
    e.birth_date, e.gender, e.email, e.phone,
    d.dept_name,
    p.position_name, p.position_level
FROM employee e
JOIN department  d ON e.dept_id     = d.dept_id
JOIN job_position p ON e.position_id = p.position_id;

-- 2. 급여 요약 뷰 (직원 + 부서 + 급여 통합)
CREATE OR REPLACE VIEW v_salary_summary AS
SELECT
    e.emp_id, e.emp_name, e.emp_no,
    d.dept_name,
    p.position_name,
    s.salary_year, s.salary_month,
    s.base_salary, s.gross_salary,
    s.total_deduction, s.net_salary,
    s.overtime_pay, s.status AS salary_status,
    s.pay_date
FROM salary s
JOIN employee    e ON s.emp_id      = e.emp_id
JOIN department  d ON e.dept_id     = d.dept_id
JOIN job_position p ON e.position_id = p.position_id;

-- 3. 휴가 현황 뷰 (직원 + 부서 + 휴가신청 + 연차현황 통합)
CREATE OR REPLACE VIEW v_leave_status AS
SELECT
    e.emp_id, e.emp_name, e.emp_no,
    d.dept_name,
    p.position_name,
    lr.leave_id, lr.leave_type, lr.half_type,
    lr.start_date, lr.end_date, lr.days,
    lr.reason, lr.status AS leave_status,
    lr.approved_at, lr.reject_reason,
    al.leave_year, al.total_days, al.used_days, al.remain_days
FROM leave_request lr
JOIN employee    e  ON lr.emp_id    = e.emp_id
JOIN department  d  ON e.dept_id    = d.dept_id
JOIN job_position p  ON e.position_id = p.position_id
LEFT JOIN annual_leave al ON e.emp_id = al.emp_id
    AND al.leave_year = YEAR(lr.start_date);

-- 4. 평가 결과 뷰 (직원 + 부서 + 직급 + 평가결과 통합)
CREATE OR REPLACE VIEW v_evaluation_result AS
SELECT
    e.emp_id, e.emp_name, e.emp_no,
    d.dept_name,
    p.position_name,
    ev.eval_id, ev.eval_year, ev.eval_period, ev.eval_type,
    ev.total_score, ev.grade, ev.eval_comment,
    ev.eval_status, ev.confirmed_at,
    evaluator.emp_name AS evaluator_name
FROM evaluation ev
JOIN employee    e         ON ev.emp_id       = e.emp_id
JOIN department  d         ON e.dept_id       = d.dept_id
JOIN job_position p         ON e.position_id   = p.position_id
LEFT JOIN employee evaluator ON ev.evaluator_id = evaluator.emp_id;