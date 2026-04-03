package com.hrms.emp.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.ApprovalDAO;
import com.hrms.emp.dto.LeaveDTO;
import com.hrms.emp.dto.ResignDTO;

public class ApprovalService {

    private ApprovalDAO approvalDao = new ApprovalDAO();

    // ────────────────────────────────────────────────
    // 부서장 여부 확인
    // ────────────────────────────────────────────────
    public boolean isDeptManager(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.isDeptManager(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 결재 현황 — 미완료
    // ────────────────────────────────────────────────

    // 부서장용 휴직/복직 목록 (미완료)
    public List<LeaveDTO> getLeaveListForDeptManager(int empId, String status,
            String keyword, String deptName, String leaveType) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveListForDeptManager(con, empId, status, keyword, deptName, leaveType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 부서장용 퇴직 목록 (미완료)
    public List<ResignDTO> getResignListForDeptManager(int empId, String status,
            String keyword, String deptName) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignListForDeptManager(con, empId, status, keyword, deptName);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // HR담당자/관리자용 휴직/복직 목록 (미완료)
    public List<LeaveDTO> getLeaveListForHr(String status,
            String keyword, String deptName, String leaveType) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveListForHr(con, status, keyword, deptName, leaveType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // HR담당자/관리자용 퇴직 목록 (미완료)
    public List<ResignDTO> getResignListForHr(String status,
            String keyword, String deptName) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignListForHr(con, status, keyword, deptName);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 결재 처리 결과 — 완료 (HR담당자/관리자용)
    // ────────────────────────────────────────────────

    // 휴직/복직 처리 결과 건수
    public int getLeaveDoneCount(String status,
            String keyword, String deptName, String leaveType) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveDoneCount(con, status, keyword, deptName, leaveType);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getLeaveDoneList(String status,
            String keyword, String deptName, String leaveType,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveDoneList(con, status, keyword, deptName, leaveType, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 퇴직 처리 결과 건수
    public int getResignDoneCount(String status,
            String keyword, String deptName) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignDoneCount(con, status, keyword, deptName);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 퇴직 처리 결과 목록
    public List<ResignDTO> getResignDoneList(String status,
            String keyword, String deptName,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignDoneList(con, status, keyword, deptName, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 결재 처리 결과 — 완료 (부서장용)
    // ────────────────────────────────────────────────

    // 부서장용 휴직/복직 처리 결과 건수
    public int getLeaveDoneCountForDeptManager(int empId, String status,
            String keyword, String deptName, String leaveType) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveDoneCountForDeptManager(con, empId, status, keyword, deptName, leaveType);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 부서장용 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getLeaveDoneListForDeptManager(int empId, String status,
            String keyword, String deptName, String leaveType,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveDoneListForDeptManager(con, empId, status, keyword, deptName, leaveType, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 부서장용 퇴직 처리 결과 건수
    public int getResignDoneCountForDeptManager(int empId, String status,
            String keyword, String deptName) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignDoneCountForDeptManager(con, empId, status, keyword, deptName);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 부서장용 퇴직 처리 결과 목록
    public List<ResignDTO> getResignDoneListForDeptManager(int empId, String status,
            String keyword, String deptName,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignDoneListForDeptManager(con, empId, status, keyword, deptName, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 내 신청 현황 — 미완료
    // ────────────────────────────────────────────────

    // 내 휴직/복직 신청 목록 (미완료)
    public List<LeaveDTO> getMyLeaveList(int empId, String leaveType, String status) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyLeaveList(con, empId, leaveType, status);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 내 퇴직 신청 목록 (미완료)
    public List<ResignDTO> getMyResignList(int empId, String status) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyResignList(con, empId, status);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 내 신청 처리 결과 — 완료
    // ────────────────────────────────────────────────

    // 내 휴직/복직 처리 결과 건수
    public int getMyLeaveDoneCount(int empId, String leaveType, String status) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyLeaveDoneCount(con, empId, leaveType, status);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 내 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getMyLeaveDoneList(int empId, String leaveType, String status,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyLeaveDoneList(con, empId, leaveType, status, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 내 퇴직 처리 결과 건수
    public int getMyResignDoneCount(int empId, String status) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyResignDoneCount(con, empId, status);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 내 퇴직 처리 결과 목록
    public List<ResignDTO> getMyResignDoneList(int empId, String status,
            int offset, int pageSize) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getMyResignDoneList(con, empId, status, offset, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ────────────────────────────────────────────────
    // 상세 조회
    // ────────────────────────────────────────────────

    // 휴직/복직 상세 조회
    public LeaveDTO getLeaveDetail(int requestId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getLeaveDetail(con, requestId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // 퇴직 상세 조회
    public ResignDTO getResignDetail(int requestId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return approvalDao.getResignDetail(con, requestId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}