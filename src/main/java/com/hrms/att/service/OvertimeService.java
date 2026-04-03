package com.hrms.att.service;

import java.sql.Connection;

import com.hrms.att.dao.OvertimeDAO;
import com.hrms.att.dto.OvertimeDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import com.hrms.sys.dao.NotificationDAO;

public class OvertimeService {

    private DeptDAO deptDAO = new DeptDAO();
    private EmpDAO empDAO = new EmpDAO();
    private OvertimeDAO overtimeDAO = new OvertimeDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();
    
    //승인자 찾기
    public int findApprover(int empId) {

        int deptId = empDAO.getDeptIdByEmpId(empId);

        while (deptId != 0) {

            DeptDTO dept = deptDAO.getDeptById(deptId);

            if (dept == null) {
                throw new RuntimeException("부서 없음");
            }

            // ⭐ 팀장 있으면 바로 반환
            if (dept.getManager_id() != 0) {
                return dept.getManager_id();
            }

            // ⭐ 없으면 상위 부서로 이동
            deptId = dept.getParent_dept_id();
        }

        throw new RuntimeException("승인자 없음");
    }
    
    //신청 + 알림
    public void applyOvertime(OvertimeDTO dto) {

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ⭐ 트랜잭션 시작

            int approverId = findApprover(dto.getEmpId());
            dto.setApproverId(approverId);
            
            // 1. 초과근무 저장
            overtimeDAO.insertOvertime(conn, dto);

            // 2. 승인자에게 알림
            notificationDAO.insert(
                dto.getApproverId(),
                "OVERTIME",                 // noti_type
                "overtime_request",         // ref_table
                null,                       // ref_id (나중에 ot_id 넣어도 좋음)
                "초과근무 신청이 도착했습니다.",
                conn
            );

            conn.commit();

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException(e);

        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    //승인 + 알림
    public void approveOvertime(int otId, int loginEmpId) {

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. 데이터 조회
            OvertimeDTO ot = overtimeDAO.findById(conn, otId);

            if (ot == null) {
                throw new RuntimeException("데이터 없음");
            }

            // 2. 권한 체크
            if (ot.getApproverId() != loginEmpId) {
                throw new RuntimeException("승인 권한 없음");
            }

            // 3. 상태 체크
            if (!"대기".equals(ot.getStatus())) {
                throw new RuntimeException("이미 처리됨");
            }

            // 4. 승인 처리
            overtimeDAO.updateStatus(conn, otId, "승인");

            // 5. 신청자에게 알림
            notificationDAO.insert(
                ot.getEmpId(),
                "OVERTIME",
                "overtime_request",
                otId, // ⭐ 이거 넣으면 클릭 이동 가능
                "초과근무가 승인되었습니다.",
                conn
            );

            conn.commit();

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException(e);

        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    //반려 + 알림
    public void rejectOvertime(int otId, int loginEmpId) {

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            OvertimeDTO ot = overtimeDAO.findById(conn, otId);

            if (ot.getApproverId() != loginEmpId) {
                throw new RuntimeException("권한 없음");
            }

            overtimeDAO.updateStatus(conn, otId, "반려");

            notificationDAO.insert(
                ot.getEmpId(),
                "OVERTIME",
                "overtime_request",
                otId,
                "초과근무가 반려되었습니다.",
                conn
            );

            conn.commit();

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException(e);

        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
