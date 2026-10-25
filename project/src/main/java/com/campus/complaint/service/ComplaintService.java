package com.campus.complaint.service;

import com.campus.complaint.dao.ComplaintDAO;
import com.campus.complaint.model.Complaint;

import java.util.List;

public class ComplaintService {
    private ComplaintDAO complaintDAO = new ComplaintDAO();
    private PriorityService priorityService = new PriorityService();

    public boolean submitComplaint(int studentId, String title, String category, String description, String location) {
        Complaint complaint = new Complaint();
        // The sequential ID (CMP-1005, CMP-1006, ...) is assigned atomically by ComplaintDAO.insert().
        complaint.setStudentId(studentId);
        complaint.setTitle(title);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setLocation(location);
        
        // Automatic Priority Calculation on Server
        String calculatedPriority = priorityService.calculatePriority(category, title, description, location);
        complaint.setPriority(calculatedPriority);
        complaint.setStatus("PENDING");

        return complaintDAO.insert(complaint);
    }

    public List<Complaint> getComplaintsForStudent(int studentId) {
        return complaintDAO.findByStudentId(studentId);
    }

    public List<Complaint> getAllComplaints() {
        return complaintDAO.findAll();
    }

    public Complaint getComplaintById(String id) {
        return complaintDAO.findById(id);
    }

    public boolean updateComplaintStatus(String id, String status, String assignedStaff) {
        return complaintDAO.updateStatusAndStaff(id, status, assignedStaff);
    }

    public int getStudentTotalCount(int studentId) {
        return complaintDAO.countByStudent(studentId);
    }

    public int getStudentPendingCount(int studentId) {
        return complaintDAO.countByStudentAndStatus(studentId, "PENDING");
    }

    public int getStudentInProgressCount(int studentId) {
        return complaintDAO.countByStudentAndStatus(studentId, "IN_PROGRESS") + complaintDAO.countByStudentAndStatus(studentId, "ASSIGNED");
    }

    public int getStudentResolvedCount(int studentId) {
        return complaintDAO.countByStudentAndStatus(studentId, "RESOLVED");
    }

    public int getTotalCount() {
        return complaintDAO.countAll();
    }

    public int getCountByStatus(String status) {
        return complaintDAO.countByStatus(status);
    }

    public int getCountByPriority(String priority) {
        return complaintDAO.countByPriority(priority);
    }
}
