package com.hrms.emp.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/emp/detail") 
public class empDetailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 2. 넘어온 사번(emp_no) 파라미터 받기 (현재는 에러 해결이 먼저니 받기만 합니다)
        String empNo = request.getParameter("emp_no");
        
        // TODO: 나중에 여기서 DAO를 호출해서 empNo에 해당하는 직원 상세 정보를 DB에서 가져와야 합니다.
        // EmpDTO empDetail = empDao.getEmpDetail(empNo);
        // request.setAttribute("empDetail", empDetail);

        // 3. 브라우저 대신 서버 내부에서 WEB-INF 안의 detail.jsp로 몰래 포워딩(연결) 해줍니다.
        request.getRequestDispatcher("/WEB-INF/jsp/emp/detail.jsp").forward(request, response);
    }
}