package com.campus.complaint.servlet;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.User;
import com.campus.complaint.service.ComplaintService;
import com.campus.complaint.service.PriorityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;

@WebServlet("/admin-dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();
    private PriorityService priorityService = new PriorityService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=Unauthorized+admin+access.");
            return;
        }

        List<Complaint> complaints = complaintService.getAllComplaints();
        
        // Demonstrate PriorityQueue sorting in-memory
        PriorityQueue<Complaint> priorityQueue = priorityService.buildPriorityQueue(complaints);

        int totalCount = complaintService.getTotalCount();
        int pendingCount = complaintService.getCountByStatus("PENDING");
        int assignedCount = complaintService.getCountByStatus("ASSIGNED");
        int inProgressCount = complaintService.getCountByStatus("IN_PROGRESS");
        int resolvedCount = complaintService.getCountByStatus("RESOLVED");
        int criticalCount = complaintService.getCountByPriority("CRITICAL");

        request.setAttribute("complaints", complaints);
        request.setAttribute("priorityQueueSize", priorityQueue.size());
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("pendingCount", pendingCount);
        request.setAttribute("assignedCount", assignedCount);
        request.setAttribute("inProgressCount", inProgressCount);
        request.setAttribute("resolvedCount", resolvedCount);
        request.setAttribute("criticalCount", criticalCount);

        request.getRequestDispatcher("/admin-dashboard.jsp").forward(request, response);
    }
}
