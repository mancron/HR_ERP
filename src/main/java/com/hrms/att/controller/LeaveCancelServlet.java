package com.hrms.att.controller;

import com.hrms.att.service.LeaveService;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;

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

        String year = request.getParameter("year");
        String month = request.getParameter("month");
        
        try {
            // 🔥 파라미터 받기
            int leaveId = Integer.parseInt(request.getParameter("id"));

            // 🔥 Service 호출
            boolean result = leaveService.cancelLeave(leaveId, empId);

            String redirectUrl = request.getContextPath() + "/att/leave/req";
            
            if (result) {
                redirectUrl += "?msg=cancel_success";
            } else {
                redirectUrl += "?error=cancel_fail";
            }

            // 🔥 month 유지
            if (year != null && month != null) {
                redirectUrl += "&year=" + year + "&month=" + month;
            }

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/att/leave/req?error=exception");
        }
    }
}