package com.hrms.main.controller;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.main.dto.MainDashboardDTO;
import com.hrms.main.service.MainService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private MainService mainService;

    @Override
    public void init() throws ServletException {
        this.mainService = new MainService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        int     empId     = (Integer) session.getAttribute("empId");
        String  role      = (String)  session.getAttribute("userRole");
        Boolean isMgrAttr = (Boolean) session.getAttribute("isManager");
        boolean isManager = (isMgrAttr != null && isMgrAttr);

        // 출퇴근 카드 — 전 역할 공통
        AttendanceDTO todayAtt = mainService.getTodayAttendance(empId);
        request.setAttribute("todayAtt", todayAtt);

        MainDashboardDTO dashboard;
        String jspPath;

        if ("관리자".equals(role)) {
            dashboard = mainService.getAdminDashboard(empId);
            jspPath   = "/WEB-INF/jsp/main/main_admin.jsp";

        } else if ("HR담당자".equals(role)) {
            dashboard = mainService.getHrDashboard(empId);
            jspPath   = "/WEB-INF/jsp/main/main_hr.jsp";

        } else if ("최종승인자".equals(role)) {
            dashboard = mainService.getCeoDashboard(empId);
            jspPath   = "/WEB-INF/jsp/main/main_ceo.jsp";

        } else if (isManager) {
            // 부서장 — role은 일반직원이지만 isManager=true
            // HR담당자 겸 부서장인 경우는 위 HR담당자 분기에서 처리됨
            dashboard = mainService.getManagerDashboard(empId);
            jspPath   = "/WEB-INF/jsp/main/main_manager.jsp";

        } else {
            // 일반직원
            dashboard = mainService.getUserDashboard(empId);
            jspPath   = "/WEB-INF/jsp/main/main_user.jsp";
        }

        request.setAttribute("dashboard", dashboard);
        request.getRequestDispatcher(jspPath).forward(request, response);
    }
}