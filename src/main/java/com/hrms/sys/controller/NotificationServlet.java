package com.hrms.sys.controller;

import com.hrms.sys.dto.NotificationDTO;
import com.hrms.sys.service.NotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/sys/notification")
public class NotificationServlet extends HttpServlet {

    private NotificationService notificationService;

    @Override
    public void init() throws ServletException {
        this.notificationService = new NotificationService();
    }

    // ── GET: 알림 목록 ──
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        int empId = (Integer) session.getAttribute("empId");

        // 필터: 전체 or 미읽음
        String filter = request.getParameter("filter");
        boolean unreadOnly = "unread".equals(filter);

        List<NotificationDTO> notiList = notificationService.getNotifications(empId, unreadOnly);
        int unreadCount = notificationService.getUnreadCount(empId);

        request.setAttribute("notiList",    notiList);
        request.setAttribute("unreadCount", unreadCount);
        request.setAttribute("filter",      filter);

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_notification.jsp")
               .forward(request, response);
    }

    // ── POST: 읽음 처리 ──
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        int empId = (Integer) session.getAttribute("empId");

        String action = request.getParameter("action");

        if ("markAll".equals(action)) {
            notificationService.markAllAsRead(empId);
        } else if ("markOne".equals(action)) {
            String notiIdParam = request.getParameter("notiId");
            if (notiIdParam != null) {
                notificationService.markAsRead(Long.parseLong(notiIdParam), empId);
            }
        }

        // PRG 패턴
        response.sendRedirect(request.getContextPath() + "/sys/notification");
    }
}