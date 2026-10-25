package com.campus.complaint.model;

import java.sql.Timestamp;

public class Complaint implements Comparable<Complaint> {
    private String id;
    private int studentId;
    private String studentName; // Joined from users table for Admin view
    private String title;
    private String category;
    private String description;
    private String location;
    private String priority; // 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    private String status;   // 'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED'
    private String assignedStaff;
    private Timestamp submissionTimestamp;
    private Timestamp updatedAt;

    public Complaint() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(String assignedStaff) { this.assignedStaff = assignedStaff; }

    public Timestamp getSubmissionTimestamp() { return submissionTimestamp; }
    public void setSubmissionTimestamp(Timestamp submissionTimestamp) { this.submissionTimestamp = submissionTimestamp; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Helper method for priority weight comparison in PriorityQueue
    public int getPriorityWeight() {
        if (priority == null) return 0;
        switch (priority) {
            case "CRITICAL": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    @Override
    public int compareTo(Complaint other) {
        // Higher priority weight comes first (descending order)
        int weightCompare = Integer.compare(other.getPriorityWeight(), this.getPriorityWeight());
        if (weightCompare != 0) {
            return weightCompare;
        }
        // Tie-breaker 1: Earlier submission time comes first (ascending timestamp)
        if (this.submissionTimestamp != null && other.submissionTimestamp != null) {
            int timeCompare = this.submissionTimestamp.compareTo(other.submissionTimestamp);
            if (timeCompare != 0) {
                return timeCompare;
            }
        }
        // Tie-breaker 2: Complaint ID alphabetical
        if (this.id != null && other.id != null) {
            return this.id.compareTo(other.id);
        }
        return 0;
    }
}
