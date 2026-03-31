package com.hrms.org.controller;

import java.io.IOException;
import java.math.BigDecimal;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.hrms.org.dao.PosDAO;
import com.hrms.org.dto.PosDTO;

@WebServlet("/org/position/edit")
public class PosEditServlet extends HttpServlet {
    private PosDAO posDao = new PosDAO();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        request.setAttribute("pos", posDao.getPositionById(id));
        request.getRequestDispatcher("/WEB-INF/jsp/org/pos_edit_form.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object empIdObj = (session != null) ? session.getAttribute("empId") : null;
        Integer actorId = (empIdObj instanceof Integer) ? (Integer) empIdObj : null;
        String role = (session != null) ? (String) session.getAttribute("userRole") : "";

        if (!"관리자".equals(role) && !"HR담당자".equals(role)) {
            response.sendError(403, "수정 권한이 없습니다.");
            return;
        }

        int positionId = Integer.parseInt(request.getParameter("position_id"));
        int newIsActive = Integer.parseInt(request.getParameter("is_active"));

        // [추가] 비활성화 시도 시 해당 직급을 가진 직원이 있는지 체크
        if (newIsActive == 0) {
            int empCount = posDao.getEmployeeCountByPosition(positionId); // DAO에 메서드 추가 필요
            if (empCount > 0) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<script>alert('해당 직급을 사용 중인 직원이 " + empCount + "명 존재하여 비활성화할 수 없습니다.'); history.back();</script>");
                return;
            }
        }

        PosDTO oldDto = posDao.getPositionById(positionId);
        PosDTO newDto = new PosDTO();
        newDto.setPosition_id(positionId);
        newDto.setBase_salary(new BigDecimal(request.getParameter("base_salary")));
        newDto.setMeal_allowance(Integer.parseInt(request.getParameter("meal_allowance")));
        newDto.setTransport_allowance(Integer.parseInt(request.getParameter("transport_allowance")));
        newDto.setPosition_allowance(Integer.parseInt(request.getParameter("position_allowance")));
        newDto.setIs_active(newIsActive);

        String[] columns = {"base_salary", "meal_allowance", "transport_allowance", "position_allowance", "is_active"};
        String[] oldValues = {
            oldDto.getBase_salary().toString(),
            String.valueOf(oldDto.getMeal_allowance()),
            String.valueOf(oldDto.getTransport_allowance()),
            String.valueOf(oldDto.getPosition_allowance()),
            String.valueOf(oldDto.getIs_active())
        };
        String[] newValues = {
            newDto.getBase_salary().toString(),
            String.valueOf(newDto.getMeal_allowance()),
            String.valueOf(newDto.getTransport_allowance()),
            String.valueOf(newDto.getPosition_allowance()),
            String.valueOf(newDto.getIs_active())
        };

        boolean result = posDao.updatePositionWithLog(newDto, actorId, columns, oldValues, newValues);

        response.setContentType("text/html;charset=UTF-8");
        if(result) {
            response.getWriter().println("<script>alert('성공적으로 수정되었으며 감사 로그가 기록되었습니다.'); parent.closeModal();</script>");
        } else {
            response.getWriter().println("<script>alert('수정 중 오류가 발생했습니다.'); history.back();</script>");
        }
    }
}