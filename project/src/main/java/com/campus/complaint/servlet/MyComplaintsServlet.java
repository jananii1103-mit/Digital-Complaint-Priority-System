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

@WebServlet("/my-complaints")
public class MyComplaintsServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        List<Complaint> complaints = complaintService.getComplaintsForStudent(user.getId());
        request.setAttribute("complaints", complaints);
        request.getRequestDispatcher("/my-complaints.jsp").forward(request, response);
    }
}
