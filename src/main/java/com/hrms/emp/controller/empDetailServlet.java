package com.hrms.emp.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.EmpService;

@WebServlet("/emp/detail") 
public class empDetailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private EmpService empService = new EmpService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        //넘어온 사번(emp_no) 파라미터 받기
        String empNo = request.getParameter("emp_no");
        
        //사번이 null이거나 비어있으면 목록으로 튕겨내는 방어 로직
        if (empNo == null || empNo.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }
        
        //Service를 호출해서 해당 사번의 상세 정보를 가져옵니다.
        EmpDTO empDetail = empService.getEmployeeDetail(empNo);
        
        //JSP에서 ${empDetail} 로 쓸 수 있게 세팅합니다.
        request.setAttribute("empDetail", empDetail);

        //브라우저 대신 서버 내부에서 WEB-INF 안의 detail.jsp로 몰래 포워딩(연결) 해줍니다.
        request.getRequestDispatcher("/WEB-INF/jsp/emp/detail.jsp").forward(request, response);
    }
}