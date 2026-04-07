package com.hrms.att.controller;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Connection;

@WebServlet("/att/annual/adjust")
public class AnnualAdjustServlet extends HttpServlet {

	private LeaveDAO leaveDAO = new LeaveDAO();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 1. 파라미터 받기
		int empId = Integer.parseInt(request.getParameter("empId"));
		double newTotal = Double.parseDouble(request.getParameter("totalDays"));

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 기존 연차 조회
			AnnualLeaveDTO current = leaveDAO.getAnnualLeave(empId);

			double oldTotal = current.getTotalDays();

			// 2. 차이 계산 (🔥 핵심)
			double adjustDays = newTotal - oldTotal;

			// 변경 없으면 종료
			if (adjustDays == 0) {
				response.sendRedirect(request.getContextPath() + "/att/annual");
				return;
			}

			// 3. total_days만 변경
			leaveDAO.adjustTotalDays(conn, empId, adjustDays);

			conn.commit();

			// 4. 알림 (commit 이후)
			NotificationUtil.sendAnnualAdjusted(empId, adjustDays);

		} catch (Exception e) {
			e.printStackTrace();

			if (conn != null) {
				try {
					conn.rollback();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		response.sendRedirect(request.getContextPath() + "/att/annual");
	}
}