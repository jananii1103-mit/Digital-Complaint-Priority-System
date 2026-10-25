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

@WebServlet("/submit-complaint")
public class SubmitComplaintServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        request.getRequestDispatcher("/submit-complaint.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String title = request.getParameter("title");
        String category = request.getParameter("category");
        String description = request.getParameter("description");
        String location = request.getParameter("location");

        request.setAttribute("submittedTitle", title);
        request.setAttribute("submittedCategory", category);
        request.setAttribute("submittedDescription", description);
        request.setAttribute("submittedLocation", location);
        if (title == null || title.trim().isEmpty() || category == null || category.trim().isEmpty() ||
            description == null || description.trim().isEmpty() || location == null || location.trim().isEmpty()) {
            request.setAttribute("errorMessage", "All fields are required.");
            request.getRequestDispatcher("/submit-complaint.jsp").forward(request, response);
            return;
        }

        try {
            boolean success = complaintService.submitComplaint(user.getId(), title.trim(), category.trim(), description.trim(), location.trim());
            if (success) {
                response.sendRedirect(request.getContextPath() + "/my-complaints?success=Complaint+submitted+successfully+with+automated+priority.");
                return;
            }
            request.setAttribute("errorMessage", "The complaint was not saved. Please try again or contact the administrator.");
        } catch (RuntimeException e) {
            // Keep database details in server logs only; never expose credentials or SQL to the browser.
            getServletContext().log("Complaint submission failed for student ID " + user.getId() + ".", e);
            request.setAttribute("errorMessage", "The complaint could not be saved because of a server or database error. Please contact the administrator.");
        }
        request.setAttribute("submittedTitle", title);
        request.setAttribute("submittedCategory", category);
        request.setAttribute("submittedDescription", description);
        request.setAttribute("submittedLocation", location);
        request.getRequestDispatcher("/submit-complaint.jsp").forward(request, response);
    }
}
