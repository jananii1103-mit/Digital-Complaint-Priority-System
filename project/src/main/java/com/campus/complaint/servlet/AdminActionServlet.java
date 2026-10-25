package com.campus.complaint.servlet;

import com.campus.complaint.model.User;
import com.campus.complaint.service.ComplaintService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/admin/update-complaint")
public class AdminActionServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        String complaintId = request.getParameter("complaintId");
        String status = request.getParameter("status");
        String assignedStaff = request.getParameter("assignedStaff");

        if (complaintId == null || complaintId.trim().isEmpty() || status == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Complaint ID and status are required.");
            return;
        }
        String normalizedStatus = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Arrays.asList("PENDING", "ASSIGNED", "IN_PROGRESS", "RESOLVED").contains(normalizedStatus)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid complaint status.");
            return;
        }
        boolean updated = complaintService.updateComplaintStatus(complaintId.trim(), normalizedStatus,
                assignedStaff == null ? null : assignedStaff.trim());
        String query = updated ? "?success=Complaint+updated+successfully." : "?error=Complaint+could+not+be+updated.";
        response.sendRedirect(request.getContextPath() + "/admin-dashboard" + query);
    }
}
