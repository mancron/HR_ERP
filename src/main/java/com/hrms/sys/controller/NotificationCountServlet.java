package com.hrms.sys.controller;

import com.hrms.sys.service.NotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/notification/count")
public class NotificationCountServlet extends HttpServlet {

    private NotificationService notificationService;

    @Override
    public void init() throws ServletException {
        this.notificationService = new NotificationService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.getWriter().write("{\"count\":0}");
            return;
        }

        int empId = (Integer) session.getAttribute("empId");
        int count = notificationService.getUnreadCount(empId);
        response.getWriter().write("{\"count\":" + count + "}");
    }
}