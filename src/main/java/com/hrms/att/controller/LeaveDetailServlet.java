package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.service.LeaveService;

@WebServlet("/leave/detail")
public class LeaveDetailServlet extends HttpServlet {

	private LeaveService leaveService = new LeaveService();

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		int leaveId = Integer.parseInt(req.getParameter("leaveId"));

		LeaveDTO dto = leaveService.getLeaveDetail(leaveId);

		resp.setContentType("application/json;charset=UTF-8");

		PrintWriter out = resp.getWriter();

		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

		out.print(gson.toJson(dto));
		
		System.out.print(gson.toJson(dto));
	}
}
