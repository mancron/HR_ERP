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
import com.hrms.org.dto.DeptDTO;
import com.hrms.org.dto.PosDTO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dao.PosDAO;

@WebServlet("/emp/list")
public class EmpListServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final EmpService empService = new EmpService();
    private final DeptDAO deptDao = new DeptDAO();
    private final PosDAO  posDao  = new PosDAO();

    // 페이징 상수
    private static final int NUM_PER_PAGE   = 10; // 페이지당 직원 수
    private static final int PAGE_PER_BLOCK =  5; // 블럭당 페이지 수

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── 1. 검색 파라미터 ──────────────────────────────────
        String keyword    = request.getParameter("keyword");
        String deptId     = request.getParameter("dept_id");
        String positionId = request.getParameter("position_id");
        String status     = request.getParameter("status");

        // status 기본값: 재직
        String selStatus = (status != null && !status.isEmpty()) ? status : "재직";

        // ── 2. 드롭다운 데이터 ────────────────────────────────
        Vector<DeptDTO> deptList = deptDao.deptList();
        Vector<PosDTO>  posList  = posDao.posList();

        // ── 3. 전체 직원 목록 조회 (필터 적용) ───────────────
        Vector<EmpDTO> allList   = empService.getEmployeeList(keyword, deptId, positionId, selStatus);
        int totalCount = (allList != null) ? allList.size() : 0;

        // ── 4. 페이징 계산 ────────────────────────────────────
        int nowPage = 1;
        try {
            String pageParam = request.getParameter("nowPage");
            if (pageParam != null && !pageParam.isEmpty()) {
                nowPage = Integer.parseInt(pageParam);
            }
        } catch (NumberFormatException e) {
            nowPage = 1;
        }

        int totalPage  = (totalCount == 0) ? 1 : (int) Math.ceil((double) totalCount / NUM_PER_PAGE);

        // 페이지 범위 보정
        if (nowPage < 1)         nowPage = 1;
        if (nowPage > totalPage) nowPage = totalPage;

        int nowBlock   = (int) Math.ceil((double) nowPage  / PAGE_PER_BLOCK);
        int totalBlock = (int) Math.ceil((double) totalPage / PAGE_PER_BLOCK);

        int pageStart = (nowBlock - 1) * PAGE_PER_BLOCK + 1;
        int pageEnd   = Math.min(pageStart + PAGE_PER_BLOCK - 1, totalPage);

        // ── 5. 현재 페이지 데이터만 잘라내기 ─────────────────
        int startIdx = (nowPage - 1) * NUM_PER_PAGE;
        int endIdx   = Math.min(startIdx + NUM_PER_PAGE, totalCount);

        Vector<EmpDTO> empList = new Vector<>();
        if (allList != null) {
            for (int i = startIdx; i < endIdx; i++) {
                empList.add(allList.get(i));
            }
        }

        // ── 6. JSP로 전달 ─────────────────────────────────────
        request.setAttribute("deptList",   deptList);
        request.setAttribute("posList",    posList);
        request.setAttribute("empList",    empList);
        request.setAttribute("totalCount", totalCount);

        // 선택값 유지 (String 타입으로 통일 → EL 비교 안전)
        request.setAttribute("selDeptId", deptId     != null ? deptId     : "all");
        request.setAttribute("selPosId",  positionId != null ? positionId : "all");
        request.setAttribute("selStatus", selStatus);

        // 페이징 관련 값
        request.setAttribute("nowPage",    nowPage);
        request.setAttribute("totalPage",  totalPage);
        request.setAttribute("nowBlock",   nowBlock);
        request.setAttribute("totalBlock", totalBlock);
        request.setAttribute("pagePerBlock", PAGE_PER_BLOCK);
        request.setAttribute("pageStart",  pageStart);
        request.setAttribute("pageEnd",    pageEnd);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/list.jsp").forward(request, response);
    }
}