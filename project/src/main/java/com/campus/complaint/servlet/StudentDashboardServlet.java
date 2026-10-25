package com.campus.complaint.servlet;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.User;
import com.campus.complaint.service.ComplaintService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/student-dashboard")
public class StudentDashboardServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=Unauthorized+access.");
            return;
        }

        int studentId = user.getId();
        int total = complaintService.getStudentTotalCount(studentId);
        int pending = complaintService.getStudentPendingCount(studentId);
        int inProgress = complaintService.getStudentInProgressCount(studentId);
        int resolved = complaintService.getStudentResolvedCount(studentId);

        List<Complaint> complaints = complaintService.getComplaintsForStudent(studentId);

        request.setAttribute("totalCount", total);
        request.setAttribute("pendingCount", pending);
        request.setAttribute("inProgressCount", inProgress);
        request.setAttribute("resolvedCount", resolved);
        request.setAttribute("complaints", complaints);

        request.getRequestDispatcher("/student-dashboard.jsp").forward(request, response);
    }
}
