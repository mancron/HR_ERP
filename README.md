# HR ERP 인사 자원 관리 시스템

> JSP + Servlet + MySQL 8.0 기반 HR ERP 웹 애플리케이션

---

## 📋 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 프로젝트명 | HR ERP 인사 자원 관리 시스템 |
| 개발 기간 | 2025년 (3주) |
| 개발 환경 | Java 17 · Tomcat 10 · MySQL 8.0 · JSP/Servlet |

---
## 🚫 개발 준수 사항 (Critical Constraints)

### 1. 절대 금지 사항
* **스크립틀릿 금지**: JSP 파일 내부(`<% %>`) 작성을 금지하며, 데이터 렌더링은 JSTL과 EL(`${}`)만 사용하라.
* **라이브러리 의존 지양**: 외부 매퍼 라이브러리 없이 `Connection`, `PreparedStatement`, `ResultSet`을 직접 제어하라.

### 2. 트랜잭션 및 예외 처리 (Transaction & Exception)
* **수동 커밋 제어**: Service 계층에서 `connection.setAutoCommit(false)`를 호출하여 트랜잭션을 시작하라.
* **원자성 보장**: 다중 테이블 DML 수행 중 예외 발생 시 `catch` 블록에서 반드시 `connection.rollback()`을 실행하라.
* **자원 해제**: `finally` 블록에서 `ResultSet`, `Statement`, `Connection`을 반드시 역순으로 `close()` 하라.
* **알림 격리**: 알림(`notification`) 처리는 메인 트랜잭션 `commit()` 완료 후 별도의 `try-catch`로 실행하여 본체 데이터에 영향을 주지 않도록 하라.

### 3. 검증 및 감사 로그 (Validation & Audit Log)
* **사전 상태 검증**: DB 제약으로 확인 불가능한 로직(휴가 중복 등)은 `INSERT/UPDATE` 실행 전 `SELECT` 쿼리로 반드시 먼저 확인하라.
* **Audit 기록**: 주요 테이블 변경 시, `UPDATE` 전 기존 값을 조회하여 `old_value`와 `new_value`를 기록하는 `INSERT` 쿼리를 트랜잭션 내에 포함하라.
* **민감 정보 마스킹**: 로그 기록 시 계좌번호 등은 `****` 처리 후 저장하라.

### 4. 데이터베이스 규칙 (Database Rules)
* **예약어 회피**: `job_position`, `target_year`, `eval_comment` 등 명세된 컬럼명을 준수하고 쿼리 시 백틱(`` ` ``)을 사용하라.
* **논리적 삭제**: 물리적 `DELETE` 대신 `is_active=0` 또는 `status='퇴직'`과 같은 상태 값 업데이트(Soft Delete)를 활용하라.
* **정밀도 유지**: Java의 `BigDecimal`과 DB의 `DECIMAL` 타입을 매핑하여 소수점 연산 오차를 방지하라.

### 5. 컨트롤러 및 라우팅 (Controller & Routing)
* **Servlet 매핑**: 모든 요청은 `@WebServlet` 기반의 `HttpServlet`에서 처리하라.
* **PRG 패턴**: `doPost`에서 데이터 처리 후에는 반드시 `sendRedirect()`를 사용하여 브라우저 새로고침에 의한 중복 제출을 막아라.
* **공통 필터**: 세션 및 권한 검증은 `AuthFilter`에서 일괄적으로 처리하라.


## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Frontend | JSP · HTML · CSS · JavaScript |
| Backend | Servlet |
| Database | MySQL 8.0 |
| DB 연동 | JDBC |
| 서버 | Apache Tomcat 10.1 |
| 빌드 | Maven |
| 버전 관리 | Git / GitHub |

---

## 📁 주요 기능

### 직원 관리
- 직원 목록 조회 (부서·직급·상태 필터)
- 직원 등록 / 상세 조회 / 정보 수정
- 퇴직 처리 · 휴직 처리 · 복직 처리
- 인사발령 (부서이동 · 승진 · 전보)
- 전체 인사발령 이력 조회

### 근태 관리
- 출퇴근 기록 (출근 · 퇴근 · 지각 자동 판단)
- 휴가 신청 · 승인 · 반려
- 초과근무 신청 · 승인
- 월별 근태 현황 · 수동 보정
- 연차 현황 · 연차 일괄 부여

### 급여 관리
- 월별 급여 자동 계산 (4대보험 · 소득세 포함)
- 급여 지급 처리
- 급여 명세서 조회 · 인쇄
- 연도별 공제율 관리

### 인사 평가
- 평가 작성 (자기평가 · 상위평가 · 동료평가)
- 등급 자동 산정 (S / A / B / C / D)
- 최종 확정 후 급여 인상 연동

### 시스템
- 권한별 메뉴 분기 (관리자 / HR담당자 / 일반)
- 알림 (휴가 승인·반려 · 급여 지급 · 비밀번호 만료 등)
- 계정 잠금 해제 · 비밀번호 초기화 · 권한 변경
- 변경 이력 조회 (audit_log)
- 법정 공휴일 관리

---

## 🗄 DB 테이블 구조 (16개)

```
조직  : job_position · department
직원  : employee · personnel_history · account
근태  : public_holiday · attendance · leave_request · annual_leave · overtime_request
급여  : deduction_rate · salary
평가  : evaluation · evaluation_item
시스템: notification · audit_log
```

---

## ⚙️ 실행 방법

### 1. 저장소 클론

```bash
git clone https://github.com/[계정명]/hr-erp.git
cd hr-erp
```

### 2. 데이터베이스 설정

```bash
mysql -u root -p

source sql/hr_erp_schema.sql
source sql/hr_erp_dummy.sql  # 더미 데이터 (선택)
```

### 3. DB 연결 설정

`src/main/resources/db.properties` 수정

```properties
db.url=jdbc:mysql://localhost:3306/hr_erp?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
db.username=root
db.password=your_password
```

### 4. Tomcat 배포 후 실행

```
http://localhost:8080/hr-erp
```

### 5. 초기 로그인 계정

| 아이디 | 비밀번호 | 권한 |
|---|---|---|
| dev.head | 1234 | 관리자 |
| lim.dr | 1234 | HR담당자 |
| jung.sw | 1234 | 일반 |
| ceo | 1234 | 최종승인자 |
> ⚠️ 최초 로그인 후 반드시 비밀번호를 변경하세요.

---

## ⚠️ 개발 주의사항

### DB 설계

**DECIMAL 자리수**

건강보험율 `0.03545`를 정확히 저장하려면 반드시 `DECIMAL(6,5)` 이상을 사용해야 합니다.
`DECIMAL(5,4)` 사용 시 `0.0355`로 잘려 매달 급여 계산 오류가 발생합니다.

**MySQL 예약어**

아래 단어는 테이블·컬럼명으로 직접 사용하면 오류가 납니다.

| 예약어 | 대체 이름 |
|---|---|
| `position` | `job_position` |
| `comment` | `eval_comment` |
| `year` | `target_year`, `leave_year` |

**부서장 FK 순환참조**

`department`와 `employee`가 서로를 참조하는 구조입니다.
SQL 실행 순서를 반드시 지켜야 합니다.

```
1. department 테이블 생성 (manager_id FK 없는 상태)
2. employee 테이블 생성
3. ALTER TABLE department ADD CONSTRAINT fk_dept_manager ...
```

---

### 트랜잭션 필수 처리

아래 기능은 반드시 단일 트랜잭션으로 처리해야 합니다.
중간 오류 발생 시 전체 롤백해야 데이터 정합성이 유지됩니다.

| 기능 | 묶어야 하는 테이블 |
|---|---|
| 직원 등록 | employee + account + annual_leave |
| 퇴직 처리 | employee + account + personnel_history + audit_log |
| 휴직/복직 | employee + personnel_history + audit_log |
| 휴가 승인 | leave_request + annual_leave + attendance |
| 급여 계산 | salary (전 직원) |
| 급여 지급 | salary + audit_log |

**알림(`notification`)은 트랜잭션 외부**에서 별도 try-catch로 처리합니다.
알림 저장 실패가 핵심 기능 롤백을 유발하지 않도록 해야 합니다.

```java
// 핵심 로직 — 트랜잭션
conn.setAutoCommit(false);
try {
    leaveMapper.updateStatus(...);
    annualLeaveMapper.updateDays(...);
    attendanceMapper.insertHoliday(...);
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
}

// 알림 — 트랜잭션 외부 (실패해도 무관)
try {
    notificationMapper.insert(...);
} catch (Exception e) {
    log.warn("알림 저장 실패 (무시): " + e.getMessage());
}
```

---

### 동시성 주의

같은 직원의 휴가를 두 관리자가 동시에 승인하면 연차가 이중 차감될 수 있습니다.
`SELECT ... FOR UPDATE`로 행 락을 걸어야 합니다.

```java
// 락 획득 후 잔여 연차 재확인
AnnualLeave al = annualLeaveMapper.selectForUpdate(empId, year);
if (al.getRemainDays() < days) {
    throw new Exception("잔여 연차 부족");
}
```

---

### 보안

- 비밀번호는 반드시 **BCrypt** 해싱 후 저장 — 평문 저장 절대 금지
- SQL Injection 방지: `PreparedStatement` 사용 — 문자열 직접 조합 금지
- 로그인 5회 연속 실패 시 계정 잠금 (`is_active = 0`)
- 퇴직 처리 시 `account.is_active = 0` 즉시 적용
- 세션 타임아웃 30분 — `AuthFilter`에서 모든 요청 세션 검증

---

### 서버에서 반드시 검증해야 할 항목

| 항목 | 이유 | 방법 |
|---|---|---|
| 휴가 기간 중복 | UNIQUE로 차단 불가 | 승인·대기 건과 날짜 overlap 쿼리 |
| 휴가 사용 일수 | 클라이언트 조작 가능 | 공휴일 제외 영업일 서버 계산 |
| 급여 평가 연동 | 작성중 평가 오연동 위험 | `eval_status = '최종확정'` 확인 |
| 부서장 퇴직 방지 | FK로 차단 불가 | `department.manager_id` 사전 확인 |
| 공휴일 출퇴근 | 클라이언트 조작 가능 | `public_holiday` 서버 조회 후 검증 |
| 부서 계층 깊이 | 자동 계산 없음 | `parent.dept_level + 1` 서버 계산 |

---

### 급여 계산 공식

```
초과근무수당  = (base_salary / 209) × overtime_hours × 1.5
국민연금      = gross_salary × 0.04500
건강보험      = gross_salary × 0.03545
장기요양보험  = 건강보험료 × 0.12950
고용보험      = gross_salary × 0.00900
소득세        = 간이세액표 기준 (별도 로직)
지방소득세    = 소득세 × 0.10
실수령액      = gross_salary - total_deduction
```

> 급여는 계산 시점의 값을 **스냅샷**으로 저장합니다.
> 공제율 변경 후에도 과거 명세서는 영향받지 않습니다.
> `status = '완료'`로 지급 처리된 급여는 수정 불가입니다.

---

### 공휴일 데이터

개발 환경은 `sql/hr_erp_schema.sql`에 포함된 2025년 데이터를 사용합니다.

운영 환경에서는 공공데이터포털 API 연동을 권장합니다.

```
API URL : https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo
인증키  : https://www.data.go.kr (무료 발급)
적재 방식: 매년 1월 배치(Scheduler)로 자동 적재
```

---

### audit_log 기록 대상

Service 레이어에서 UPDATE 후 직접 INSERT해야 합니다.

| 테이블 | 추적 컬럼 | 이유 |
|---|---|---|
| employee | base_salary, status, resign_date | 급여·퇴직 오류 추적 |
| account | role, is_active | 보안 감사 |
| annual_leave | used_days, remain_days | 연차 분쟁 대비 |
| salary | status (대기→완료) | 지급 처리 추적 |

---

## 👥 팀원 역할

| 이름 | 담당 업무 |
|:---:|:---|
| **김은빈** | 공통 기능 (로그인, 세션, 권한), 조직 관리 (부서, 직급) |
| **김성백** | 직원 관리 (등록, 발령, 이력) |
| **박세준** | 근태 관리 (출퇴근, 휴가)  |
| **전기현** | DB 설계 총괄, 대시보드 및 최종 통합, Text-to-SQL 챗봇 연동급여 관리 (계산 로직, 명세서) |

---
## 파일구조 (권장)

```
hr-erp/
│
├── src/main/java/com/hrms/
│   ├── common/                          # 공통 기능
│   │   ├── config/                      # 설정 클래스
│   │   │   ├── DatabaseConfig.java
│   │   │   └── AppConfig.java
│   │   ├── constants/                   # 상수 정의
│   │   │   ├── AppConstants.java        # 전역 상수
│   │   │   ├── ErrorMessages.java       # 에러 메시지
│   │   │   └── ResultCode.java          # 결과 코드
│   │   ├── exception/                   # 사용자 정의 예외
│   │   │   ├── ServiceException.java
│   │   │   ├── DAOException.java
│   │   │   └── ValidationException.java
│   │   ├── filter/                      # 서블릿 필터
│   │   │   ├── AuthenticationFilter.java  # 인증 필터
│   │   │   ├── XSSFilter.java             # XSS 방어
│   │   │   └── EncodingFilter.java        # 인코딩 필터
│   │   ├── security/                    # 보안 관련
│   │   │   ├── PasswordEncoder.java      # 비밀번호 암호화
│   │   │   ├── TokenManager.java         # 토큰 관리
│   │   │   └── SecurityUtil.java
│   │   ├── util/                        # 유틸리티 클래스
│   │   │   ├── DateUtil.java
│   │   │   ├── StringUtil.java
│   │   │   ├── ValidationUtil.java       # 입력값 검증
│   │   │   ├── ExcelUtil.java            # Excel 처리
│   │   │   └── LogUtil.java
│   │   └── db/                          # 데이터베이스 공통
│   │       ├── ConnectionPool.java
│   │       └── GenericDAO.java           # 제네릭 DAO 기반 클래스
│   │
│   ├── auth/                            # 인증 모듈 (AUTH)
│   │   ├── controller/
│   │   │   ├── LoginServlet.java         # (AUTH-01) 로그인
│   │   │   ├── LogoutServlet.java
│   │   │   └── PasswordChangeServlet.java # (AUTH-02) 비밀번호 변경
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   └── PasswordService.java
│   │   ├── dao/
│   │   │   └── AccountDAO.java
│   │   └── dto/
│   │       ├── AccountDTO.java
│   │       ├── LoginRequestDTO.java
│   │       └── LoginResponseDTO.java
│   │
│   ├── org/                             # 조직 관리 모듈 (ORG)
│   │   ├── controller/
│   │   │   ├── DepartmentServlet.java    # (ORG-01) 부서 관리
│   │   │   └── PositionServlet.java      # (ORG-02) 직급 관리
│   │   ├── service/
│   │   │   ├── DepartmentService.java
│   │   │   └── PositionService.java
│   │   ├── dao/
│   │   │   ├── DepartmentDAO.java
│   │   │   └── PositionDAO.java
│   │   └── dto/
│   │       ├── DepartmentDTO.java
│   │       └── PositionDTO.java
│   │
│   ├── emp/                             # 직원 관리 모듈 (EMP)
│   │   ├── controller/
│   │   │   ├── EmployeeListServlet.java    # (EMP-01) 직원 목록
│   │   │   ├── EmployeeDetailServlet.java  # (EMP-02) 직원 상세·수정
│   │   │   ├── EmployeeRegisterServlet.java # (EMP-03) 직원 등록
│   │   │   ├── EmployeeResignServlet.java   # (EMP-04) 퇴직 처리
│   │   │   ├── EmployeeTransferServlet.java # (EMP-05) 인사발령
│   │   │   ├── EmployeeLeaveServlet.java    # (EMP-06) 휴직 처리
│   │   │   ├── EmployeeReinstateServlet.java # (EMP-07) 복직 처리
│   │   │   └── EmployeeHistoryServlet.java  # (EMP-08) 인사발령 이력
│   │   ├── service/
│   │   │   ├── EmployeeService.java         # 직원 CRUD
│   │   │   ├── EmployeeTransferService.java # 인사발령 서비스
│   │   │   └── EmployeeLeaveService.java    # 휴직/복직 서비스
│   │   ├── dao/
│   │   │   ├── EmployeeDAO.java
│   │   │   ├── EmployeeTransferDAO.java
│   │   │   └── EmployeeLeaveOfAbsenceDAO.java
│   │   └── dto/
│   │       ├── EmployeeDTO.java
│   │       ├── EmployeeTransferDTO.java
│   │       └── EmployeeLeaveDTO.java
│   │
│   ├── att/                             # 근태 관리 모듈 (ATT)
│   │   ├── controller/
│   │   │   ├── AttendanceServlet.java        # (ATT-01) 출퇴근
│   │   │   ├── LeaveRequestServlet.java      # (ATT-02) 휴가 신청
│   │   │   ├── LeaveApprovalServlet.java     # (ATT-03) 휴가 승인
│   │   │   ├── OvertimeServlet.java          # (ATT-04) 초과근무
│   │   │   ├── AttendanceStatusServlet.java  # (ATT-05) 근태 현황·보정
│   │   │   ├── AnnualStatusServlet.java      # (ATT-06) 연차 현황
│   │   │   └── AnnualGrantServlet.java       # (ATT-07) 연차 일괄 부여
│   │   ├── service/
│   │   │   ├── AttendanceService.java        # 출퇴근 로직
│   │   │   ├── LeaveService.java             # 휴가 신청·승인 로직
│   │   │   ├── OvertimeService.java          # 초과근무 로직
│   │   │   ├── AnnualLeaveService.java       # 연차 관리 로직
│   │   │   └── AttendanceCalcService.java    # 근태 계산 로직
│   │   ├── dao/
│   │   │   ├── AttendanceDAO.java
│   │   │   ├── LeaveRequestDAO.java
│   │   │   ├── OvertimeRequestDAO.java
│   │   │   └── HolidayDAO.java
│   │   └── dto/
│   │       ├── AttendanceDTO.java
│   │       ├── LeaveRequestDTO.java
│   │       ├── OvertimeRequestDTO.java
│   │       └── HolidayDTO.java
│   │
│   ├── sal/                             # 급여 관리 모듈 (SAL)
│   │   ├── controller/
│   │   │   ├── SalaryCalculationServlet.java # (SAL-01) 급여 계산·지급
│   │   │   ├── SalarySlipServlet.java        # (SAL-02) 급여 명세서
│   │   │   ├── SalaryStatusServlet.java      # (SAL-03) 급여 현황
│   │   │   └── DeductionServlet.java         # (SAL-04) 공제율 관리
│   │   ├── service/
│   │   │   ├── SalaryService.java            # 급여 CRUD
│   │   │   ├── SalaryCalculationService.java # 급여 계산 로직
│   │   │   └── DeductionService.java         # 공제율 관리
│   │   ├── dao/
│   │   │   ├── SalaryHistoryDAO.java
│   │   │   └── SalaryDeductionRuleDAO.java
│   │   └── dto/
│   │       ├── SalaryHistoryDTO.java
│   │       └── SalaryDeductionRuleDTO.java
│   │
│   ├── eval/                            # 평가 관리 모듈 (EVAL)
│   │   ├── controller/
│   │   │   ├── EvaluationWriteServlet.java   # (EVAL-01) 평가 작성·확정
│   │   │   └── EvaluationStatusServlet.java  # (EVAL-02) 평가 현황
│   │   ├── service/
│   │   │   ├── EvaluationService.java
│   │   │   └── EvaluationCalcService.java    # 평가점수 계산
│   │   ├── dao/
│   │   │   └── EvaluationDAO.java
│   │   └── dto/
│   │       └── EvaluationDTO.java
│   │
│   └── sys/                             # 시스템 관리 모듈 (SYS)
│       ├── controller/
│       │   ├── NotificationServlet.java       # (SYS-01) 알림
│       │   ├── AccountUnlockServlet.java      # (SYS-02) 계정 잠금 해제
│       │   ├── HolidayManageServlet.java      # (SYS-03) 공휴일 관리
│       │   ├── AuditLogServlet.java           # (SYS-04) 변경 이력
│       │   ├── PasswordResetServlet.java      # (SYS-05) 비밀번호 초기화
│       │   └── RoleChangeServlet.java         # (SYS-06) 계정 권한 변경
│       ├── service/
│       │   ├── NotificationService.java
│       │   ├── HolidayService.java
│       │   ├── AuditLogService.java
│       │   └── RoleService.java
│       ├── dao/
│       │   ├── NotificationDAO.java
│       │   ├── HolidayDAO.java
│       │   ├── AuditLogDAO.java
│       │   └── RoleDAO.java
│       └── dto/
│           ├── NotificationDTO.java
│           ├── AuditLogDTO.java
│           └── RoleDTO.java
│
├── src/main/webapp/WEB-INF/
│   ├── WEB-INF/jsp/
│   │   ├── layout/                      # 레이아웃 JSP
│   │   │   ├── header.jsp
│   │   │   ├── sidebar.jsp
│   │   │   ├── footer.jsp
│   │   │   └── base-layout.jsp
│   │   ├── common/                      # 공통 JSP
│   │   │   ├── error.jsp
│   │   │   ├── sidebar.jsp
│   │   │   ├── header.jsp
│   │   │   └── loading.jsp
│   │   │
│   │   ├── auth/                        # 인증 화면
│   │   │   ├── auth-login.jsp           # (AUTH-01) 로그인
│   │   │   └── auth-password-change.jsp # (AUTH-02) 비밀번호 변경
│   │   │
│   │   ├── org/                         # 조직 관리 화면
│   │   │   ├── org-department-list.jsp  # (ORG-01) 부서 관리
│   │   │   └── org-position-list.jsp    # (ORG-02) 직급 관리
│   │   │
│   │   ├── emp/                         # 직원 관리 화면
│   │   │   ├── emp-list.jsp             # (EMP-01) 직원 목록
│   │   │   ├── emp-detail.jsp           # (EMP-02) 직원 상세·수정
│   │   │   ├── emp-register.jsp         # (EMP-03) 직원 등록
│   │   │   ├── emp-resign.jsp           # (EMP-04) 퇴직 처리
│   │   │   ├── emp-transfer.jsp         # (EMP-05) 인사발령
│   │   │   ├── emp-leave.jsp            # (EMP-06) 휴직 처리
│   │   │   ├── emp-reinstate.jsp        # (EMP-07) 복직 처리
│   │   │   └── emp-transfer-history.jsp # (EMP-08) 인사발령 이력
│   │   │
│   │   ├── att/                         # 근태 관리 화면
│   │   │   ├── att-attendance.jsp       # (ATT-01) 출퇴근
│   │   │   ├── att-leave-request.jsp    # (ATT-02) 휴가 신청
│   │   │   ├── att-leave-approval.jsp   # (ATT-03) 휴가 승인
│   │   │   ├── att-overtime.jsp         # (ATT-04) 초과근무
│   │   │   ├── att-status.jsp           # (ATT-05) 근태 현황·보정
│   │   │   ├── att-annual-status.jsp    # (ATT-06) 연차 현황
│   │   │   └── att-annual-grant.jsp     # (ATT-07) 연차 일괄 부여
│   │   │
│   │   ├── sal/                         # 급여 관리 화면
│   │   │   ├── sal-calculation.jsp      # (SAL-01) 급여 계산·지급
│   │   │   ├── sal-slip.jsp             # (SAL-02) 급여 명세서
│   │   │   ├── sal-status.jsp           # (SAL-03) 급여 현황
│   │   │   └── sal-deduction.jsp        # (SAL-04) 공제율 관리
│   │   │
│   │   ├── eval/                        # 평가 관리 화면
│   │   │   ├── eval-write.jsp           # (EVAL-01) 평가 작성·확정
│   │   │   └── eval-status.jsp          # (EVAL-02) 평가 현황
│   │   │
│   │   └── sys/                         # 시스템 관리 화면
│   │       ├── sys-notification.jsp     # (SYS-01) 알림
│   │       ├── sys-account-unlock.jsp   # (SYS-02) 계정 잠금 해제
│   │       ├── sys-holiday.jsp          # (SYS-03) 공휴일 관리
│   │       ├── sys-audit.jsp            # (SYS-04) 변경 이력
│   │       ├── sys-password-reset.jsp   # (SYS-05) 비밀번호 초기화
│   │       └── sys-role.jsp             # (SYS-06) 계정 권한 변경
│   │
│   ├── css/
│   │   ├── common.css                   # 공통 스타일
│   │   ├── layout.css                   # 레이아웃 스타일
│   │   ├── form.css                     # 폼 스타일
│   │   ├── table.css                    # 테이블 스타일
│   │   └── responsive.css               # 반응형 스타일
│   │
│   ├── js/
│   │   ├── common.js                    # 공통 자바스크립트
│   │   ├── validation.js                # 입력값 검증
│   │   ├── api-client.js                # API 호출
│   │   ├── datetime-picker.js           # 날짜/시간 선택기
│   │   └── chart.js                     # 차트 라이브러리
│   │
│   ├── images/
│   │   ├── logo.png
│   │   ├── icons/
│   │   └── backgrounds/
│   │
│   └── WEB-INF/
│       ├── web.xml                      # 웹 애플리케이션 설정
│       ├── lib/                         # 외부 라이브러리
│       └── classes/                     # 컴파일된 클래스
│
├── sql/
│   ├── 00_init/
│   │   ├── hr_erp_schema.sql            # 테이블 생성 (메인)
│   │   └── hr_erp_dummy.sql             # 더미 데이터
│   │
│   ├── 01_auth/
│   │   └── account.sql
│   │
│   ├── 02_org/
│   │   ├── job_position.sql
│   │   └── department.sql
│   │
│   ├── 03_emp/
│   │   ├── employee.sql
│   │   ├── employee_transfer.sql
│   │   └── employee_leave_of_absence.sql
│   │
│   ├── 04_att/
│   │   ├── attendance.sql
│   │   ├── leave_request.sql
│   │   ├── overtime_request.sql
│   │   └── holiday.sql
│   │
│   ├── 05_sal/
│   │   ├── salary_history.sql
│   │   └── salary_deduction_rule.sql
│   │
│   ├── 06_eval/
│   │   └── evaluation.sql
│   │
│   └── 07_sys/
│       ├── audit_log.sql
│       ├── role.sql
│       └── notification.sql
│
├── docs/
│   ├── 01_프로젝트_개요.md               # 프로젝트 설명
│   ├── 02_아키텍처.md                   # 아키텍처 가이드
│   ├── 03_네이밍_규칙.md                # 네이밍 컨벤션
│   ├── 04_코딩_규칙.md                  # 코딩 스타일 가이드
│   ├── 05_DB_설계.md                    # 데이터베이스 설계
│   ├── 06_API_명세.md                   # API 명세서
│   ├── 07_보안_정책.md                  # 보안 정책
│   └── 08_배포_가이드.md                # 배포 절차
│
├── pom.xml                             # Maven 설정 (또는 build.gradle)
└── README.md                           # 프로젝트 README
```
attendance
## 📄 관련 문서

```
specs/
├── Database Design Document.pdf     # 데이터베이스 설계 명세서 v4
├── Functional Specification Document.pdf      # 기능 설계서 (33개 화면)
└── Screen Design Document.html         # 화면 설계 (인터랙티브 프로토타입)

database/
├── hr_erp_schema.sql            # DB 스키마 생성 + 초기 데이터
└── hr_erp_dummy.sql             # 더미 데이터
```

## 📌 DB 트리거 운영 및 제거 가이드 (자기 승인 방지)

현재 휴가(`leave_request`) 및 초과근무(`overtime_request`) 결재 시 **기안자가 본인 건을 직접 승인하는 것을 방지**하기 위해 DB 트리거가 적용되어 있습니다.

### 1. 적용된 트리거 목록
* `check_leave_self_approve` (INSERT 방지)
* `check_leave_self_approve_update` (UPDATE 방지)
* `check_ot_self_approve` (INSERT 방지)
* `check_ot_self_approve_update` (UPDATE 방지)

### 2. 도입 배경
MySQL 8.0.16 이상 버전에서 `CHECK` 제약조건과 `FOREIGN KEY (ON DELETE SET NULL)`을 동일 컬럼에 사용할 경우 발생하는 문법 충돌(Error 3823)을 우회하기 위한 조치입니다.

### 3. 🚨 삭제 시 주의사항 (즉시 삭제 금지)
비즈니스 로직이 DB 계층에 종속되는 것은 장기적인 유지보수 관점에서 **권장하지 않는 안 좋은 설계**입니다. 하지만, 현재 이 트리거들은 잘못된 승인을 막는 시스템의 유일한 방어선입니다. 백엔드 검증 없이 당장 삭제할 경우 심각한 데이터 무결성 훼손이 발생할 수 있습니다.

### 4. 안전한 제거를 위한 마이그레이션 단계
향후 유지보수성을 위해 아래 순서대로 로직을 애플리케이션(API) 계층으로 이관한 뒤 트리거를 제거해야 합니다.

1. **백엔드 검증 추가**: API 서비스 레이어에서 승인 요청 처리 시 `emp_id`와 `approver_id`가 동일하면 예외(Exception)를 발생시키도록 구현.
2. **프론트엔드 제어**: 클라이언트 UI에서 본인 기안 건은 '승인' 버튼 자체를 비활성화 처리.
3. **트리거 완전 삭제**: 위 1, 2번 작업이 배포되고 100% 방어가 검증된 시점에 DB에서 일괄 `DROP TRIGGER` 실행.





rag 폴더 이동
cd hr-erp/rag

가상환경 생성
python -m venv venv

가상환경 활성화
venv\Scripts\activate # Windows

source venv/bin/activate # Mac/Linux

패키지 설치
pip install fastapi uvicorn chromadb sentence-transformers

pip install mysql-connector-python requests python-dotenv

# ChromaDB 구축 (최초 1회)
python vector_store.py --rebuild

venv\Scripts\activate
python rag_server.py


## RAG 기반 Text-to-SQL 설명

---

### 한 줄 요약
> **"자연어로 질문하면 AI가 SQL을 짜서 DB를 조회해주는 기능"**

---

### 사용 기술

| 분류 | 기술 | 역할 |
|---|---|---|
| LLM | Qwen2.5-Coder 3B | 자연어 → SQL 생성 |
| 임베딩 | multilingual-e5-small | 텍스트 → 숫자 벡터 변환 |
| 벡터 DB | ChromaDB | 유사 예제 저장 및 검색 |
| RAG 서버 | Python FastAPI | 벡터 검색 전용 마이크로서비스 |
| 메인 서버 | Java Servlet | 전체 흐름 오케스트레이션 |

---

### 동작 방식

**① 사용자가 자연어로 질문**
```
"이번달 지각 제일 많이 한 부장 누구야?"
```

**② RAG 서버가 유사한 예제를 찾아줌**
```
질문을 숫자 벡터로 변환한 뒤
미리 저장해둔 예제 40개와 유사도 비교
→ 가장 비슷한 예제 SQL 2개 반환

예) "부서별 직급별 이번달 지각 횟수" (유사도 92%)
    "부장들 중 오늘 출근시간이 가장 빠른 사람" (유사도 85%)
```

**③ 프롬프트 조립**
```
DB 테이블 구조(DDL)
+ 도메인 규칙 (지각=status='지각' 등)
+ RAG가 찾아준 예제 SQL 2개
+ 사용자 원본 질문
= 하나의 완성된 프롬프트
```

**④ LLM이 SQL 생성**
```
Qwen2.5-Coder가 위 프롬프트를 보고
우리 DB 구조에 맞는 SQL을 생성
→ SELECT e.emp_name, COUNT(*) AS late_count ...
```

**⑤ 안전 검증 후 DB 실행**
```
SELECT만 허용 (INSERT/UPDATE/DELETE 차단)
읽기전용 DB 계정으로만 실행
최대 500행 제한
→ 결과를 화면에 테이블로 출력
```

---

### RAG를 쓰는 이유

LLM 단독으로 사용시 문제:

```
❌ LLM 단독:
   "지각 많은 부장" 질문
   → employee 테이블에 dept_name 컬럼이 없는데 있다고 착각
   → 잘못된 SQL 생성 → 에러

✅ RAG 적용:
   유사 예제 "부서별 지각 횟수" SQL을 참고
   → v_employee_full 뷰 사용 패턴을 학습
   → 올바른 SQL 생성
```

**RAG는 LLM이 우리 DB 구조를 더 잘 이해하도록 관련 예시를 골라서 먼저 보여주는 역할**.

---

### 완전 로컬 동작의 장점

```
외부 API 사용 안 함 (ChatGPT API 등 불필요)
→ 인터넷 끊겨도 동작
→ 사내 민감 데이터가 외부로 나가지 않음
→ API 비용 없음
```

---

### 시스템 구성도

```
[사용자]
    │ 자연어 질문
    ▼
[Java 서버]
    │
    ├─→ [Python RAG 서버]  ← CPU 동작 (가벼움)
    │       └─ ChromaDB에서 유사 예제 검색
    │       └─ 예제 + 카테고리 반환
    │
    ├─→ 프롬프트 조립 (DDL + 규칙 + 예제 + 질문)
    │
    ├─→ [Ollama LLM]  ← GPU 동작
    │       └─ SQL 생성
    │
    └─→ [MySQL]  ← 읽기전용 실행
            └─ 결과 반환 → 화면 출력
```



### RAG란?

**RAG (Retrieval-Augmented Generation)**
**검색 증강 생성** 

---

### RAG의 3단계 원리

**① Retrieve (검색)**
```
질문을 숫자 벡터로 변환
→ 미리 저장된 예제들과 유사도 비교
→ 가장 관련있는 예제 추출
```

**② Augment (보강)**
```
추출한 예제를 LLM 프롬프트에 추가
→ "이런 비슷한 예제가 있어, 참고해서 답해줘"
```

**③ Generate (생성)**
```
LLM이 예제를 참고해서
우리 DB에 맞는 정확한 SQL 생성
```

---

### 왜 단순히 예제를 하드코딩 안 하냐?

```
하드코딩 방식 (기존):
  "지각" 키워드 → 지각 관련 예제 무조건 삽입
  "부장" 키워드 → 부장 관련 예제 무조건 삽입
  → 키워드가 없거나 애매하면 엉뚱한 예제 삽입

RAG 방식 (현재):
  "이번달 지각 제일 많은 부장" 질문 전체 의미를 벡터로 변환
  → 의미가 가장 가까운 예제를 동적으로 선택
  → 새로운 질문 패턴도 유연하게 대응
  → 예제 DB에 추가만 하면 코드 수정 없이 확장 가능
```

---

### 이 프로젝트에서 RAG 구성요소

```
저장소 (무엇을 검색하나):
  HR 도메인 전용 예제 Q&A 41개
  "부장 목록" → SELECT ... FROM v_employee_full ...
  "이번달 지각자" → SELECT ... WHERE status = '지각' ...

검색 엔진 (어떻게 찾나):
  multilingual-e5-small 모델로 텍스트를 벡터화
  ChromaDB에서 코사인 유사도로 가장 가까운 예제 탐색

활용 (찾은걸 어떻게 쓰나):
  찾아낸 예제를 프롬프트에 삽입
  → LLM이 예제 패턴을 보고 SQL 생성
```

---

### 한 줄 정리

> RAG는 **"AI가 답하기 전에 관련 자료를 먼저 찾아보게 만드는 기술"** ,
> 이 프로젝트에서는 자연어 질문과 가장 비슷한 예제 SQL을 먼저 찾아서 LLM에게 보여줌으로써 DB에 맞는 정확한 SQL을 생성하도록 유도하는 데 사용.


