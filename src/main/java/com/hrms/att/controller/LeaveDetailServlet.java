package com.hrms.att.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.att.service.LeaveService;

@WebServlet("/att/leave/detail")
public class LeaveDetailServlet extends HttpServlet {

    private LeaveService service = new LeaveService();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        int id = Integer.parseInt(req.getParameter("id"));

        LeaveDTO dto = service.getLeaveDetail(id);

        RequestDTO r = new RequestDTO();

        r.setId(dto.getLeaveId());
        r.setDate(dto.getStartDate() + " ~ " + dto.getEndDate());
        r.setType(dto.getLeaveType());
        r.setStatus(dto.getStatus());
        r.setReason(dto.getReason());
        r.setApplyDate(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "-");

        r.setEmpName(dto.getEmpName());
        r.setApproverName(dto.getApproverName());
        r.setApproveDate(dto.getApprovedAt() != null ? dto.getApprovedAt().toString() : "-");
        r.setRejectReason(dto.getRejectReason());

        resp.setContentType("application/json;charset=UTF-8");

        new Gson().toJson(r, resp.getWriter());
    }
}