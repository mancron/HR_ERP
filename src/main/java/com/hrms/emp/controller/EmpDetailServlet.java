package com.hrms.emp.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.EmpService;

@WebServlet("/emp/detail") 
public class EmpDetailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private EmpService empService = new EmpService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
    	//세션에서 필요한 값 직접 가져오기
    	HttpSession session = request.getSession();
    	String userRole = (String) session.getAttribute("userRole"); // "최종승인자", "일반" 등
        Integer loginEmpId = (Integer) session.getAttribute("empId");  // 로그인한 사람의 고유 ID
        
        //세션 만료시 페이지로 이동
        if (userRole == null) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            // 부모 창(window.top)의 위치를 로그인 페이지로 변경
            out.println("<script>");
            out.println("alert('세션이 만료되었습니다. 다시 로그인해주세요.');");
            out.println("window.top.location.href='" + request.getContextPath() + "/auth/login';");
            out.println("</script>");
            out.flush();
            return;
        }
        
        //파라미터 및 데이터 조회 (기존 로직)
        String empNo = request.getParameter("emp_no");
        if (empNo == null || empNo.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }
        
        EmpDTO empDetail = empService.getEmployeeDetail(empNo);
        if (empDetail == null) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }
        
        //로그인 여부 및 권한 체크
        if (userRole == null || loginEmpId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        
        int targetEmpId = empDetail.getEmp_id(); // 상세보기 대상의 ID
        boolean canAccess = false;
        
        // 권한 논리: 관리자급이거나, 일반 사원이면서 자기 자신의 정보인 경우
        if ("최종승인자".equals(userRole) || "HR담당자".equals(userRole)) {
            canAccess = true;
        } else if (loginEmpId.equals(targetEmpId)) {
            canAccess = true;
        }
        
        //권한이 없으면 빈페이지
        if (!canAccess) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<html><body style='display:flex; align-items:center; justify-content:center; height:100vh; font-family:sans-serif;'>");
            out.println("<p style='color:gray; font-size:1rem;'>열람 권한이 없습니다.</p>");
            out.println("</body></html>");
            out.flush();
            return;
        }
        
        //JSP에서 ${empDetail} 로 쓸 수 있게 세팅합니다.
        request.setAttribute("empDetail", empDetail);
        request.setAttribute("userRole", userRole); //서블릿에서 userRole세션을 꺼내 JSP에 넘길 수 있게 세팅
        request.setAttribute("loginEmpId", loginEmpId); 
        //브라우저 대신 서버 내부에서 WEB-INF 안의 detail.jsp로 몰래 포워딩
        request.getRequestDispatcher("/WEB-INF/jsp/emp/detail.jsp").forward(request, response);
    }
}