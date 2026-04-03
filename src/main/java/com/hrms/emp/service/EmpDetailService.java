package com.hrms.emp.service;

import java.sql.Connection;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;

public class EmpDetailService {

	private EmpDAO empDao = new EmpDAO();

    //권한별 필드 필터링 후 DB 업데이트
    public int updateEmployee(EmpDTO updateDto, String userRole) {
    	
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 현재 DB 값 먼저 조회
            EmpDTO current = empDao.getEmpDetail(con, updateDto.getEmp_no());
            if (current == null) return 0;

            // 공통 수정 가능 필드 (권한 무관)
            current.setEmp_name(updateDto.getEmp_name());
            current.setGender(updateDto.getGender());
            current.setBirth_date(updateDto.getBirth_date());
            current.setPhone(updateDto.getPhone());
            current.setEmail(updateDto.getEmail());
            current.setEmergency_contact(updateDto.getEmergency_contact());
            current.setBank_account(updateDto.getBank_account());
            current.setAddress(updateDto.getAddress());

            // 관리자 / HR담당자만 수정 가능한 필드
            if ("관리자".equals(userRole) || "HR담당자".equals(userRole)) {
                current.setEmp_type(updateDto.getEmp_type());
                current.setBase_salary(updateDto.getBase_salary());
            }
            // 일반 사원은 위 두 필드를 current(DB 원본값) 그대로 유지

            int result = empDao.updateEmp(con, current);
            con.commit();
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
}
