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
| hong | password123 | 관리자 |
| yoon_sj | password123 | HR담당자 |
| kim_mj | password123 | 일반 |

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

| 이름 | 담당 |
|---|---|
| 김은빈 | 조직 관리 · 직원 관리 · 인사 평가 · DB 설계 |
| 김성백 | 직원 등록 · 인사발령 · 퇴직 처리 |
| 박세준 | 근태 관리 · 급여 명세 · 시스템 |
| 전기현 | DB 설계 · 공제율 관리 · 감사 로그 |

---

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
