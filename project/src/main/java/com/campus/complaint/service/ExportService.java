package com.campus.complaint.service;

import com.campus.complaint.model.Complaint;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {
    public String exportComplaintsToCSV(List<Complaint> complaints, String exportFilePath) throws IOException {
        File file = new File(exportFilePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Unable to create export directory.");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Complaint ID,Student Name,Category,Title,Location,Priority,Status,Assigned Staff,Submission Date");
            writer.newLine();
            if (complaints != null) {
                for (Complaint c : complaints) {
                    writer.write(csv(c.getId())); writer.write(',');
                    writer.write(csv(c.getStudentName() == null ? "Unknown" : c.getStudentName())); writer.write(',');
                    writer.write(csv(c.getCategory())); writer.write(',');
                    writer.write(csv(c.getTitle())); writer.write(',');
                    writer.write(csv(c.getLocation())); writer.write(',');
                    writer.write(csv(c.getPriority())); writer.write(',');
                    writer.write(csv(c.getStatus())); writer.write(',');
                    writer.write(csv(c.getAssignedStaff() == null ? "Unassigned" : c.getAssignedStaff())); writer.write(',');
                    writer.write(csv(c.getSubmissionTimestamp() == null ? "" : c.getSubmissionTimestamp().toString()));
                    writer.newLine();
                }
            }
        }
        return file.getAbsolutePath();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        // Reduce spreadsheet formula injection for untrusted user-provided fields.
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
