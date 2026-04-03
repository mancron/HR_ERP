package com.hrms.att.controller;

import com.hrms.att.dto.OvertimeDTO;
import com.hrms.att.service.OvertimeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;

@WebServlet("/att/overtime/req")
public class OvertimeRequestServlet extends HttpServlet {

    private OvertimeService service = new OvertimeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 신청 화면으로 이동
        request.getRequestDispatcher("/WEB-INF/jsp/att/overtimeRequest.jsp")
               .forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // 1. 로그인 사용자
            HttpSession session = request.getSession();
            Integer empId = (Integer) session.getAttribute("empId");

            if (empId == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            // 2. 파라미터 받기
            String dateStr = request.getParameter("otDate");
            String startStr = request.getParameter("startTime");
            String endStr = request.getParameter("endTime");
            String reason = request.getParameter("reason");

            // 3. 유효성 체크
            if (dateStr == null || startStr == null || endStr == null || reason == null) {
                throw new RuntimeException("입력값 누락");
            }

            // 4. 타입 변환
            Date otDate = Date.valueOf(dateStr);
            Time startTime = Time.valueOf(startStr + ":00");
            Time endTime = Time.valueOf(endStr + ":00");

            // 5. 시간 검증
            if (startTime.after(endTime)) {
                throw new RuntimeException("종료시간 오류");
            }

            // 6. 근무시간 계산
            long diff = endTime.getTime() - startTime.getTime();
            double hours = diff / (1000.0 * 60 * 60);

            // 7. DTO 생성
            OvertimeDTO dto = new OvertimeDTO();
            dto.setEmpId(empId);
            dto.setOtDate(otDate);
            dto.setStartTime(startTime);
            dto.setEndTime(endTime);
            dto.setOtHours(hours);
            dto.setReason(reason);

            // 8. 서비스 호출 (트랜잭션 + 승인자 자동 지정)
            service.applyOvertime(dto);

            // 9. 성공 후 이동
            response.sendRedirect(request.getContextPath() + "/att/overtime/req");

        } catch (Exception e) {
            e.printStackTrace();

            request.setAttribute("errorMessage", "초과근무 신청 실패");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp")
                   .forward(request, response);
        }
    }
}
