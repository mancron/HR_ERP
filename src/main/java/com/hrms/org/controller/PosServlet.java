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
        // 모든 사용자가 목록 조회 가능
        request.setAttribute("posList", posDao.posListFull());
        request.setAttribute("viewPage", "/WEB-INF/jsp/org/position.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}