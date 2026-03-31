package com.hrms.auth.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;


@WebServlet("/auth/check-session")
public class CheckSessionServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // false를 인자로 주어 세션이 없어도 새로 생성하지 않도록 함
        HttpSession session = request.getSession(false); 
        boolean isAlive = (session != null && session.getAttribute("empId") != null);
        
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().print("{\"isAlive\": " + isAlive + "}");
    }
}