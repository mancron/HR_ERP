package com.hrms.org.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.hrms.org.dao.PosDAO;

@WebServlet("/org/position")
public class PosServlet extends HttpServlet {
    private PosDAO posDao = new PosDAO();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	HttpSession session = request.getSession(false); // 기존 세션이 없으면 null 반환
        
        // 로그인 정보(예: loginUser)가 세션에 없으면 로그인 페이지로 쫓아냄
        if (session == null || session.getAttribute("loginUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login"); 
            return; // 리다이렉트 후 코드 실행 중단 필수!
        }
    	
    	// 모든 사용자가 목록 조회 가능
        request.setAttribute("posList", posDao.posListFull());
        request.setAttribute("viewPage", "/WEB-INF/jsp/org/position.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}