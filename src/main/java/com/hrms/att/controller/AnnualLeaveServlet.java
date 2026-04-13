package com.hrms.att.controller;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.emp.dao.EmpDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/att/annual")
public class AnnualLeaveServlet extends HttpServlet {

	private LeaveDAO dao = new LeaveDAO();
	private EmpDAO edao = new EmpDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. 파라미터
        String yearParam = request.getParameter("year");
        String dept = request.getParameter("dept");
        String name = request.getParameter("name");

        int year = (yearParam == null)
                ? LocalDate.now().getYear()
                : Integer.parseInt(yearParam);

        int page = 1;
        int size = 10;

        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            page = Integer.parseInt(pageParam.trim());
        }

        int offset = (page - 1) * size;
        
        int totalCount = dao.getAnnualLeaveCount(year, dept, name);
        int totalPage = (int) Math.ceil((double) totalCount / size);
        
        // 2. 데이터 조회
        List<AnnualLeaveDTO> list = dao.getAnnualLeaveList(year, dept, name, offset, size);
        List<Integer> yearList = dao.getAvailableYears();
        List<String> deptList = edao.getDeptList();

        // 3. request 저장
        request.setAttribute("list", list);
        request.setAttribute("yearList", yearList);
        request.setAttribute("deptList", deptList);

        request.setAttribute("year", year);
        request.setAttribute("dept", dept);
        request.setAttribute("name", name);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPage", totalPage);

        // 4. 이동
        request.getRequestDispatcher("/WEB-INF/jsp/att/annualList.jsp")
               .forward(request, response);
    }
}