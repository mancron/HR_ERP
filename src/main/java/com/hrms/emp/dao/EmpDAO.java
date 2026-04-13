package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dto.EmpDTO;

public class EmpDAO {

	public EmpDAO() {
	}

	/**
	 * 필터 조건으로 직원 목록 조회 Connection은 Service에서 받아옵니다 (트랜잭션 관리를 위해).
	 */
	public Vector<EmpDTO> searchEmpList(Connection con, String keyword, int deptId, int positionId, String status) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Vector<EmpDTO> vlist = new Vector<>();

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT e.*, d.dept_name, p.position_name ");
			sql.append("FROM employee e ");
			sql.append("LEFT JOIN department d ON e.dept_id = d.dept_id ");
			sql.append("LEFT JOIN job_position p ON e.position_id = p.position_id ");
			sql.append("WHERE 1=1 ");

			if (keyword != null && !keyword.trim().isEmpty()) {
				sql.append("AND (e.emp_name LIKE ? OR e.emp_no LIKE ?) ");
			}
			if (deptId > 0) {
				sql.append("AND e.dept_id = ? ");
			}
			if (positionId > 0) {
				sql.append("AND e.position_id = ? ");
			}
			// status가 null이거나 "all"이면 필터 안 함
			if (status != null && !status.isEmpty() && !status.equals("all")) {
				sql.append("AND e.status = ? ");
			}

			sql.append("ORDER BY e.emp_no ASC");

			pstmt = con.prepareStatement(sql.toString());

			int idx = 1;
			if (keyword != null && !keyword.trim().isEmpty()) {
				pstmt.setString(idx++, "%" + keyword.trim() + "%");
				pstmt.setString(idx++, "%" + keyword.trim() + "%");
			}
			if (deptId > 0) {
				pstmt.setInt(idx++, deptId);
			}
			if (positionId > 0) {
				pstmt.setInt(idx++, positionId);
			}
			if (status != null && !status.isEmpty() && !status.equals("all")) {
				pstmt.setString(idx++, status);
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				vlist.addElement(mapRow(rs));
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (pstmt != null)
					pstmt.close();
				// ※ Connection은 Service에서 닫습니다. 여기서 닫지 않습니다.
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return vlist;
	}

	/**
	 * 사번으로 직원 상세 조회
	 */
	public EmpDTO getEmpDetail(Connection con, String empNo) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		EmpDTO dto = null;

		String sql = "SELECT e.*, d.dept_name, p.position_name " + "FROM employee e "
				+ "LEFT JOIN department d ON e.dept_id = d.dept_id "
				+ "LEFT JOIN job_position p ON e.position_id = p.position_id " + "WHERE e.emp_no = ?";

		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, empNo);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				dto = mapRow(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (pstmt != null)
					pstmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dto;
	}

	/**
	 * ResultSet → EmpDTO 매핑 (중복 제거용 내부 메서드)
	 */
	private EmpDTO mapRow(ResultSet rs) throws Exception {
		EmpDTO dto = new EmpDTO();
		dto.setEmp_id(rs.getInt("emp_id"));
		dto.setEmp_name(rs.getString("emp_name"));
		dto.setEmp_no(rs.getString("emp_no"));
		dto.setDept_id(rs.getInt("dept_id"));
		dto.setPosition_id(rs.getInt("position_id"));
		dto.setHire_date(rs.getString("hire_date"));
		dto.setResign_date(rs.getString("resign_date"));
		dto.setEmp_type(rs.getString("emp_type"));
		dto.setStatus(rs.getString("status"));
		dto.setBase_salary(rs.getInt("base_salary"));
		dto.setBirth_date(rs.getString("birth_date"));
		dto.setGender(rs.getString("gender"));
		dto.setAddress(rs.getString("address"));
		dto.setEmergency_contact(rs.getString("emergency_contact"));
		dto.setBank_account(rs.getString("bank_account"));
		dto.setEmail(rs.getString("email"));
		dto.setPhone(rs.getString("phone"));
		dto.setCreated_at(rs.getString("created_at"));
		dto.setDept_name(rs.getString("dept_name"));
		dto.setPosition_name(rs.getString("position_name"));
		return dto;
	}

	/**
	 * 세션에 담을 상세 정보 조회 (부서명 포함) DB에서 사원 ID로 검색하여 모든 정보를 EmpDTO에 담아 반환합니다.
	 */
	public EmpDTO getEmployeeById(int empId) {
		EmpDTO emp = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;

		// SQL 쿼리 문법 오류 수정 및 정렬
		String sql = "SELECT e.*, d.dept_name, p.position_name " + "FROM employee e "
				+ "LEFT JOIN department d ON e.dept_id = d.dept_id "
				+ "LEFT JOIN job_position p ON e.position_id = p.position_id " + "WHERE e.emp_id = ?";

		try {
			conn = DatabaseConnection.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, empId);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				// 이미 작성하신 mapRow 메서드를 호출하면 모든 필드가 자동으로 매핑됩니다.
				emp = mapRow(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// try-with-resources 대신 기존 finally 블록 스타일로 자원 해제
			try {
				if (rs != null)
					rs.close();
				if (pstmt != null)
					pstmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return emp;
	}

	// 직원 정보 수정
	public int updateEmp(Connection con, EmpDTO dto) {
		PreparedStatement pstmt = null;

		String sql = "UPDATE employee SET " + "emp_name=?, gender=?, birth_date=?, phone=?, email=?, "
				+ "emergency_contact=?, bank_account=?, address=?, " + "emp_type=?, base_salary=? " + "WHERE emp_id=?";
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, dto.getEmp_name());
			pstmt.setString(2, dto.getGender());
			pstmt.setString(3, dto.getBirth_date());
			pstmt.setString(4, dto.getPhone());
			pstmt.setString(5, dto.getEmail());
			pstmt.setString(6, dto.getEmergency_contact());
			pstmt.setString(7, dto.getBank_account());
			pstmt.setString(8, dto.getAddress());
			pstmt.setString(9, dto.getEmp_type());
			pstmt.setInt(10, dto.getBase_salary());
			pstmt.setInt(11, dto.getEmp_id());

			return pstmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getDeptIdByEmpId(int empId) {
		String sql = "SELECT dept_id FROM employee WHERE emp_id = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getInt("dept_id");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	// 부서조회 할때 부서 추출
	public List<String> getDeptList() {

		String sql = "SELECT DISTINCT d.dept_name " + "FROM employee e " + "JOIN department d ON e.dept_id = d.dept_id "
				+ "ORDER BY d.dept_name ";

		List<String> list = new ArrayList<>();

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				list.add(rs.getString("dept_name"));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return list;
	}

	// 재직중인 인원들 검색 - 근태 보정 조건 검사할 때 사용
	public List<Integer> getAllEmpIds() {

		String sql = "SELECT emp_id FROM employee WHERE status = '재직'";

		List<Integer> list = new ArrayList<>();

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				list.add(rs.getInt("emp_id"));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return list;
	}

	//페이징용 직원 리스트 출력
	public Vector<EmpDTO> searchEmpListPaging(
	        Connection conn,
	        String keyword, int deptId, int positionId, String status,
	        int offset, int size) {

	    Vector<EmpDTO> list = new Vector<>();

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT e.*, d.dept_name, p.position_name ");
	    sql.append("FROM employee e ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("JOIN job_position p ON e.position_id = p.position_id ");
	    sql.append("WHERE 1=1 ");

	    if (keyword != null && !keyword.isEmpty()) {
	        sql.append("AND e.emp_name LIKE ? ");
	    }

	    if (deptId != 0) {
	        sql.append("AND e.dept_id = ? ");
	    }

	    if (positionId != 0) {
	        sql.append("AND e.position_id = ? ");
	    }

	    if (status != null && !status.isEmpty()) {
	        sql.append("AND e.status = ? ");
	    }

	    sql.append("ORDER BY e.emp_id ");
	    sql.append("LIMIT ?, ? ");

	    try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        if (keyword != null && !keyword.isEmpty()) {
	            pstmt.setString(idx++, "%" + keyword + "%");
	        }

	        if (deptId != 0) {
	            pstmt.setInt(idx++, deptId);
	        }

	        if (positionId != 0) {
	            pstmt.setInt(idx++, positionId);
	        }

	        if (status != null && !status.isEmpty()) {
	            pstmt.setString(idx++, status);
	        }

	        pstmt.setInt(idx++, offset);
	        pstmt.setInt(idx++, size);

	        ResultSet rs = pstmt.executeQuery();

	        while (rs.next()) {
	            EmpDTO dto = new EmpDTO();

	            dto.setEmp_id(rs.getInt("emp_id"));
	            dto.setEmp_name(rs.getString("emp_name"));
	            dto.setDept_name(rs.getString("dept_name"));
	            dto.setPosition_name(rs.getString("position_name"));

	            list.add(dto);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    return list;
	}
	
	//페이징용 카운트
	public int getEmpCount(Connection conn,
	        String keyword, int deptId, int positionId, String status) {

	    int count = 0;

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT COUNT(*) ");
	    sql.append("FROM employee e ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("JOIN job_position p ON e.position_id = p.position_id ");
	    sql.append("WHERE 1=1 ");

	    // 🔥 이름 검색
	    if (keyword != null && !keyword.isEmpty()) {
	        sql.append("AND e.emp_name LIKE ? ");
	    }

	    // 🔥 부서
	    if (deptId != 0) {
	        sql.append("AND e.dept_id = ? ");
	    }

	    // 🔥 직급
	    if (positionId != 0) {
	        sql.append("AND e.position_id = ? ");
	    }

	    // 🔥 상태
	    if (status != null && !status.isEmpty()) {
	        sql.append("AND e.status = ? ");
	    }

	    try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        if (keyword != null && !keyword.isEmpty()) {
	            pstmt.setString(idx++, "%" + keyword + "%");
	        }

	        if (deptId != 0) {
	            pstmt.setInt(idx++, deptId);
	        }

	        if (positionId != 0) {
	            pstmt.setInt(idx++, positionId);
	        }

	        if (status != null && !status.isEmpty()) {
	            pstmt.setString(idx++, status);
	        }

	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            count = rs.getInt(1);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("직원 수 조회 실패", e);
	    }

	    return count;
	}
	
	//HR담당자 리스트 출력 - 휴가, 초과근무 신청할 시 알림 발송할때 사용
	public List<Integer> getHRList() {

	    List<Integer> list = new ArrayList<>();

	    String sql = "SELECT emp_id FROM account WHERE role = 'HR담당자'";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            list.add(rs.getInt("emp_id"));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	
	//HR담당자인지 확인
	public String getRoleByEmpId(int empId) {

	    String sql = "SELECT role FROM account WHERE emp_id = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setInt(1, empId);

	        try (ResultSet rs = ps.executeQuery()) {
	            if (rs.next()) {
	                return rs.getString("role");
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return null;
	}
	
}