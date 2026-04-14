package com.hrms.att.service;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.hrms.att.dao.AnnualDAO;
import com.hrms.att.dto.AnnualGrantDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;

public class AnnualService {

	private EmpDAO empDAO = new EmpDAO();
	private AnnualDAO annualDAO = new AnnualDAO();

	// 🔥 연차 계산
	public int calculateAnnualLeave(LocalDate hireDate) {

	    int years = LocalDate.now().getYear() - hireDate.getYear();

	    // 1년 미만
	    if (years < 1) {
	        long months = ChronoUnit.MONTHS.between(hireDate, LocalDate.now());
	        return (int) Math.min(months, 11);
	    }

	    int leave = 15;

	    if (years >= 3) {
	        leave += (years - 1) / 2;
	    }

	    return Math.min(leave, 25);
	}

	// 🔥 화면 데이터 생성 (핵심)
	public Map<String, List<AnnualGrantDTO>> getGrantPageData(int deptId) {

		Map<String, List<AnnualGrantDTO>> result = new HashMap<>();

		List<AnnualGrantDTO> notGranted = new ArrayList<>();
		List<AnnualGrantDTO> granted = new ArrayList<>();

		int year = LocalDate.now().getYear();

		try (Connection con = DatabaseConnection.getConnection()) {

			Vector<EmpDTO> empList = empDAO.searchEmpList(con, null, deptId, 0, "재직");

			for (EmpDTO emp : empList) {

				if (emp.getHire_date() == null)
					continue;

				LocalDate hireDate = LocalDate.parse(emp.getHire_date());

				int years = LocalDate.now().getYear() - hireDate.getYear();

				AnnualGrantDTO dto = new AnnualGrantDTO();

				dto.setEmpId(emp.getEmp_id());
				dto.setEmpName(emp.getEmp_name());
				dto.setDeptName(emp.getDept_name());
				dto.setPositionName(emp.getPosition_name());
				dto.setHireDate(hireDate);
				dto.setYears(years);

				if (annualDAO.existsAnnual(emp.getEmp_id(), year)) {

					// 🔥 DB에서 가져오기
					int totalDays = annualDAO.getTotalDays(emp.getEmp_id(), year);
					dto.setAnnualDays(totalDays);

					granted.add(dto);

				} else {

					// 🔥 계산값 사용
					int calculated = calculateAnnualLeave(hireDate);
					dto.setAnnualDays(calculated);

					notGranted.add(dto);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		result.put("notGranted", notGranted);
		result.put("granted", granted);

		return result;
	}

	// 부서 리스트
	public List<String> getDeptList() {
		return empDAO.getDeptList();
	}

	// 미부여 인원만 연차 부여
	public void grantAnnualLeave(int deptId) {

		int year = LocalDate.now().getYear();

		try (Connection con = DatabaseConnection.getConnection()) {

			Vector<EmpDTO> empList = empDAO.searchEmpList(con, null, deptId, 0, "재직");

			for (EmpDTO emp : empList) {

				if (emp.getHire_date() == null)
					continue;

				// 🔥 이미 있으면 skip
				if (annualDAO.existsAnnual(emp.getEmp_id(), year))
					continue;

				LocalDate hireDate = LocalDate.parse(emp.getHire_date());

				int totalDays = calculateAnnualLeave(hireDate);

				annualDAO.insertAnnualLeave(emp.getEmp_id(), year, totalDays);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}