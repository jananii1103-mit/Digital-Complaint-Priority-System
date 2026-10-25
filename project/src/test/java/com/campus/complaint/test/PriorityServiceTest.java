package com.campus.complaint.test;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.service.PriorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

public class PriorityServiceTest {
    private PriorityService priorityService;

    @BeforeEach
    public void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    public void testCalculatePriorityCritical() {
        String priority = priorityService.calculatePriority("Electrical", "Exposed wires in lab", "Live electrical wires hanging out.", "Room 302");
        assertEquals("CRITICAL", priority);
    }

    @Test
    public void testExposedLiveElectricalWireInOccupiedClassroomCritical() {
        String priority = priorityService.calculatePriority("Classroom", "Exposed live electrical wire in occupied classroom", "Dangerous live wire hanging near student desks in Room 102.", "Classroom 102");
        assertEquals("CRITICAL", priority);
    }

    @Test
    public void testCalculatePriorityHigh() {
        String priority = priorityService.calculatePriority("Water", "Pipe burst", "Major water pipe leakage in corridor.", "Cafeteria");
        assertEquals("HIGH", priority);
    }

    @Test
    public void testCalculatePriorityMedium() {
        String priority = priorityService.calculatePriority("Classroom", "Fan rattling", "Ceiling fan making rattling noise.", "Lab 101");
        assertEquals("MEDIUM", priority);
    }

    @Test
    public void testCalculatePriorityLow() {
        String priority = priorityService.calculatePriority("Cleaning and Maintenance", "Dusty floor", "Corridor needs sweeping.", "Hostel A");
        assertEquals("LOW", priority);
    }

    @Test
    public void testPriorityQueueOrdering() {
        Complaint c1 = new Complaint();
        c1.setId("CMP-1");
        c1.setPriority("LOW");
        c1.setSubmissionTimestamp(Timestamp.valueOf("2026-06-01 10:00:00"));

        Complaint c2 = new Complaint();
        c2.setId("CMP-2");
        c2.setPriority("CRITICAL");
        c2.setSubmissionTimestamp(Timestamp.valueOf("2026-06-01 10:05:00"));

        Complaint c3 = new Complaint();
        c3.setId("CMP-3");
        c3.setPriority("HIGH");
        c3.setSubmissionTimestamp(Timestamp.valueOf("2026-06-01 09:00:00"));

        List<Complaint> list = Arrays.asList(c1, c2, c3);
        PriorityQueue<Complaint> pq = priorityService.buildPriorityQueue(list);

        assertEquals("CRITICAL", pq.poll().getPriority());
        assertEquals("HIGH", pq.poll().getPriority());
        assertEquals("LOW", pq.poll().getPriority());
    }
}
