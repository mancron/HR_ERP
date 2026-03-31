package com.hrms.sys.controller;

import com.hrms.sys.dto.HolidayDTO;
import com.hrms.sys.service.HolidayService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/sys/holiday")
public class HolidayManageServlet extends HttpServlet {

    private HolidayService holidayService;

    @Override
    public void init() throws ServletException {
        this.holidayService = new HolidayService();
    }

    // ─────────────────────────────────────────────
    // GET: 연도별 공휴일 목록 조회
    // ─────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 연도 파라미터 없으면 현재 연도로 기본값
        int currentYear = LocalDate.now().getYear();
        int selectedYear = currentYear;
        String yearParam = request.getParameter("year");
        if (yearParam != null && !yearParam.trim().isEmpty()) {
            try {
                selectedYear = Integer.parseInt(yearParam.trim());
            } catch (NumberFormatException e) {
                selectedYear = currentYear;
            }
        }

        // 1회성 메시지 처리 (PRG 후 세션 경유)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String successMsg = (String) session.getAttribute("successMsg");
            String errorMsg   = (String) session.getAttribute("errorMsg");
            if (successMsg != null) {
                request.setAttribute("successMsg", successMsg);
                session.removeAttribute("successMsg");
            }
            if (errorMsg != null) {
                request.setAttribute("errorMsg", errorMsg);
                session.removeAttribute("errorMsg");
            }
        }

        List<HolidayDTO> holidayList = holidayService.getHolidaysByYear(selectedYear);

        request.setAttribute("holidayList",  holidayList);
        request.setAttribute("selectedYear", selectedYear);
        request.setAttribute("currentYear",  currentYear);

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_holiday.jsp")
               .forward(request, response);
    }

    // ─────────────────────────────────────────────
    // POST: 추가 / 삭제 / API 적재 처리
    // ─────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── 권한 이중 검증 ──
        HttpSession session = request.getSession(false);
        if (session == null || !"관리자".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action     = request.getParameter("action");
        String yearParam  = request.getParameter("year");
        int    year       = (yearParam != null && !yearParam.trim().isEmpty())
                            ? Integer.parseInt(yearParam.trim())
                            : LocalDate.now().getYear();

        try {
            if ("add".equals(action)) {
                // ── 공휴일 수동 추가 ──
                String holidayDate = request.getParameter("holidayDate");
                String holidayName = request.getParameter("holidayName");

                if (holidayDate == null || holidayDate.trim().isEmpty()
                 || holidayName == null || holidayName.trim().isEmpty()) {
                    session.setAttribute("errorMsg", "날짜와 공휴일명을 모두 입력해주세요.");
                } else {
                    holidayService.addHoliday(holidayDate.trim(), holidayName.trim());
                    session.setAttribute("successMsg", "공휴일이 추가되었습니다.");
                }

            } else if ("delete".equals(action)) {
                // ── 공휴일 삭제 ──
                String idParam = request.getParameter("holidayId");
                if (idParam != null && !idParam.trim().isEmpty()) {
                    holidayService.deleteHoliday(Integer.parseInt(idParam.trim()));
                    session.setAttribute("successMsg", "공휴일이 삭제되었습니다.");
                }

            } else if ("fetchApi".equals(action)) {
                // 서비스키 미설정 체크
                if (HolidayService.isApiKeyMissing()) {
                    session.setAttribute("errorMsg",
                        "API 서비스키가 설정되지 않았습니다. .env 파일의 HOLIDAY_API_KEY를 확인해주세요.");
                } else {
                    int saved = holidayService.fetchAndSaveFromApi(year);
                    session.setAttribute("successMsg",
                        year + "년 공휴일 API 적재 완료 — 새로 추가된 " + saved + "건");
                }
            }

        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        // PRG 패턴: 처리 후 GET으로 리다이렉트
        response.sendRedirect(request.getContextPath() + "/sys/holiday?year=" + year);
    }
}