package com.hrms.att.controller;

import com.hrms.att.service.LeaveService;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/att/leave/cancel")
public class LeaveCancelServlet extends HttpServlet {

    private LeaveService leaveService = new LeaveService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        EmpDTO loginUser = (EmpDTO) session.getAttribute("loginUser");

        // 🔒 로그인 체크
        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.do");
            return;
        }

        int empId = loginUser.getEmp_id();

        try {
            // 🔥 파라미터 받기
            int leaveId = Integer.parseInt(request.getParameter("leave_id"));

            // 🔥 Service 호출
            boolean result = leaveService.cancelLeave(leaveId, empId);

            // 🔥 결과 처리
            if (result) {
                response.sendRedirect(request.getContextPath() + "/att/leave/req?msg=cancel_success");
            } else {
                response.sendRedirect(request.getContextPath() + "/att/leave/req?error=cancel_fail");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/att/leave/req?error=exception");
        }
    }
}