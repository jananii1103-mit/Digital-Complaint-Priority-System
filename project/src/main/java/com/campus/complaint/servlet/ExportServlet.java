package com.campus.complaint.servlet;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.User;
import com.campus.complaint.service.ComplaintService;
import com.campus.complaint.service.ExportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/export-complaints")
public class ExportServlet extends HttpServlet {
    private ComplaintService complaintService = new ComplaintService();
    private ExportService exportService = new ExportService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        List<Complaint> complaints = complaintService.getAllComplaints();
        File exportFile = File.createTempFile("complaints_backup_report_", ".csv");
        String filePath = exportFile.getAbsolutePath();

        try {
            exportService.exportComplaintsToCSV(complaints, filePath);
            
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"complaints_backup_report.csv\";");
            
            try (FileInputStream in = new FileInputStream(new File(filePath));
                 var out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            getServletContext().log("Complaint CSV export failed", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export failed. Check server logs.");
            }
        } finally {
            if (exportFile.exists() && !exportFile.delete()) exportFile.deleteOnExit();
        }
    }
}
