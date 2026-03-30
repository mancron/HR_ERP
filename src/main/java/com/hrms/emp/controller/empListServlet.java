package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Vector;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.EmpService;

@WebServlet("/emp/list")
public class empListServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private EmpService empService = new EmpService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//Service를 호출해 DB 데이터(직원 목록)를 가져옴
        Vector<EmpDTO> empList = empService.getEmployeeList();
		
		//JSP에서 ${empList} 로 꺼내 쓸 수 있도록 request에 담음
        request.setAttribute("empList", empList);
		
		// emp/list.jsp로 이동
		request.getRequestDispatcher("/WEB-INF/jsp/emp/list.jsp").forward(request, response);
	}

}
