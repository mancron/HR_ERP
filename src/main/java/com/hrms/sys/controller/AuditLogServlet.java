package com.hrms.sys.controller;

import com.hrms.sys.dto.AuditLogDTO;
import com.hrms.sys.service.AuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/sys/auditLog")
public class AuditLogServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private AuditLogService auditLogService;

    @Override
    public void init() throws ServletException {
        // 서블릿 초기화 시 Service 인스턴스 생성 (의존성 주입)
        this.auditLogService = new AuditLogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. 파라미터 추출
        String targetTable = request.getParameter("target_table");
        String startDate = request.getParameter("start_date");
        String endDate = request.getParameter("end_date");

        // 2. 비즈니스 로직 호출
        List<AuditLogDTO> logList = auditLogService.getAuditLogs(targetTable, startDate, endDate);

        // 3. 뷰단으로 데이터 전달
        request.setAttribute("logList", logList);
        
        // 4. JSP 포워딩
        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_audit.jsp").forward(request, response);
    }
}