package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.hrms.att.dto.AnnualGrantDTO;
import com.hrms.att.service.AnnualService;

@WebServlet("/att/annual/grant")
public class AnnualGrantServlet extends HttpServlet {

	private AnnualService service = new AnnualService();
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {

	    int deptId = 0;

	    String deptParam = request.getParameter("deptId");
	    if (deptParam != null && !deptParam.isEmpty()) {
	        deptId = Integer.parseInt(deptParam);
	    }

	    Map<String, List<AnnualGrantDTO>> data = service.getGrantPageData(deptId);

	    request.setAttribute("notGranted", data.get("notGranted"));
	    request.setAttribute("granted", data.get("granted"));

	    // 부서 리스트도 넘겨야 함
	    request.setAttribute("deptList", service.getDeptList());

	    request.getRequestDispatcher("/WEB-INF/jsp/att/annualGrant.jsp")
	           .forward(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {

	    int deptId = 0;

	    String deptParam = request.getParameter("deptId");
	    if (deptParam != null && !deptParam.isEmpty()) {
	        deptId = Integer.parseInt(deptParam);
	    }

	    service.grantAnnualLeave(deptId);

	    response.sendRedirect(request.getContextPath() + "/att/annual/grant?success=1");
	}
}