package com.campus.complaint.dao;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {

    private static volatile boolean sequenceReady = false;

    /**
     * Initializes the complaint ID counter without rewriting any existing complaint IDs.
     * A MySQL named lock serializes initialization across application instances. Existing
     * complaints are read only; only the dedicated sequence row may be inserted/advanced.
     */
    private static synchronized void ensureSequence() throws SQLException {
        if (sequenceReady) return;
        try (Connection conn = DBConnection.getConnection(); Statement setup = conn.createStatement()) {
            setup.execute("CREATE TABLE IF NOT EXISTS complaint_sequence (id TINYINT PRIMARY KEY, `last_value` INT NOT NULL) ENGINE=InnoDB");
            boolean lockAcquired = false;
            try {
                try (ResultSet lock = setup.executeQuery("SELECT GET_LOCK('complaint_seq_init', 30)")) {
                    lockAcquired = lock.next() && lock.getInt(1) == 1;
                }
                if (!lockAcquired) throw new SQLException("Could not acquire complaint ID sequence initialization lock.");

                int maxExistingId = 0;
                try (Statement query = conn.createStatement();
                     ResultSet rs = query.executeQuery("SELECT COALESCE(MAX(CAST(SUBSTRING(id, 5) AS UNSIGNED)), 0) " +
                         "FROM complaints WHERE id REGEXP '^CMP-[0-9]+$'")) {
                    if (rs.next()) maxExistingId = rs.getInt(1);
                }
                int initialCounter = Math.max(1004, maxExistingId);
                try (PreparedStatement init = conn.prepareStatement(
                        "INSERT INTO complaint_sequence (id, `last_value`) VALUES (1, ?) " +
                        "ON DUPLICATE KEY UPDATE `last_value` = GREATEST(`last_value`, VALUES(`last_value`))")) {
                    init.setInt(1, initialCounter);
                    init.executeUpdate();
                }
                sequenceReady = true;
            } finally {
                if (lockAcquired) {
                    try (ResultSet ignored = setup.executeQuery("SELECT RELEASE_LOCK('complaint_seq_init')")) {
                        if (ignored.next()) ignored.getInt(1);
                    }
                }
            }
        }
    }

    /**
     * Inserts a complaint and assigns it the next sequential reference ID (CMP-1005, CMP-1006, ...).
     * The counter row in complaint_sequence is incremented and the complaint is inserted in ONE
     * transaction on one connection. The UPDATE takes a row lock, so concurrent submissions are
     * serialized and can never receive the same number; if the insert fails, the counter
     * increment is rolled back so no number is skipped.
     * The generated ID is written back into the Complaint object.
     */
    public boolean insert(Complaint complaint) {
        String bumpSql = "UPDATE complaint_sequence SET `last_value` = LAST_INSERT_ID(`last_value` + 1) WHERE id = 1";
        String sql = "INSERT INTO complaints (id, student_id, title, category, description, location, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            ensureSequence();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize the complaint ID sequence in the configured database.", e);
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long next;
                try (Statement st = conn.createStatement()) {
                    if (st.executeUpdate(bumpSql) != 1) {
                        throw new SQLException("complaint_sequence row is missing. Run db.sql first.");
                    }
                    try (ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
                        rs.next();
                        next = rs.getLong(1);
                    }
                }
                String newId = "CMP-" + next;

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, newId);
                    pstmt.setInt(2, complaint.getStudentId());
                    pstmt.setString(3, complaint.getTitle());
                    pstmt.setString(4, complaint.getCategory());
                    pstmt.setString(5, complaint.getDescription());
                    pstmt.setString(6, complaint.getLocation());
                    pstmt.setString(7, complaint.getPriority());
                    pstmt.setString(8, complaint.getStatus() != null ? complaint.getStatus() : "PENDING");
                    if (pstmt.executeUpdate() != 1) {
                        throw new SQLException("The complaint insert did not create exactly one row.");
                    }
                }
                conn.commit();
                complaint.setId(newId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save complaint to the configured database.", e);
        }
    }

    public Complaint findById(String id) {
        // Reading must not trigger the complaint-ID migration.
        String sql = "SELECT c.*, u.full_name AS student_name FROM complaints c LEFT JOIN users u ON c.student_id = u.id WHERE c.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToComplaint(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load complaint " + id + " from the configured database.", e);
        }
        return null;
    }

    public List<Complaint> findByStudentId(int studentId) {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name AS student_name FROM complaints c LEFT JOIN users u ON c.student_id = u.id WHERE c.student_id = ? ORDER BY c.submission_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToComplaint(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load complaints for student ID " + studentId + " from the configured database.", e);
        }
        return list;
    }

    public List<Complaint> findAll() {
        // Do not run schema/ID migration as a side effect of rendering the dashboard.
        // LEFT JOIN keeps complaint rows visible if a legacy database has a missing student row.
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name AS student_name FROM complaints c LEFT JOIN users u ON c.student_id = u.id ORDER BY c.submission_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToComplaint(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load complaints from the configured database.", e);
        }
        return list;
    }

    public boolean updateStatusAndStaff(String complaintId, String status, String assignedStaff) {
        String sql = "UPDATE complaints SET status = ?, assigned_staff = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setString(2, assignedStaff);
            pstmt.setString(3, complaintId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int countByStudentAndStatus(int studentId, String status) {
        String sql = "SELECT COUNT(*) FROM complaints WHERE student_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to count complaints for student and status.", e);
        }
        return 0;
    }

    public int countByStudent(int studentId) {
        String sql = "SELECT COUNT(*) FROM complaints WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to count complaints for student.", e);
        }
        return 0;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM complaints";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to count all complaints.", e);
        }
        return 0;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM complaints WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to count complaints by status.", e);
        }
        return 0;
    }

    public int countByPriority(String priority) {
        String sql = "SELECT COUNT(*) FROM complaints WHERE priority = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, priority);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to count complaints by priority.", e);
        }
        return 0;
    }

    private Complaint mapResultSetToComplaint(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getString("id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentName(rs.getString("student_name"));
        c.setTitle(rs.getString("title"));
        c.setCategory(rs.getString("category"));
        c.setDescription(rs.getString("description"));
        c.setLocation(rs.getString("location"));
        c.setPriority(rs.getString("priority"));
        c.setStatus(rs.getString("status"));
        c.setAssignedStaff(rs.getString("assigned_staff"));
        c.setSubmissionTimestamp(rs.getTimestamp("submission_timestamp"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
