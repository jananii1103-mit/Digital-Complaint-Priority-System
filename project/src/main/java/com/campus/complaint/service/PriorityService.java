package com.campus.complaint.service;

import com.campus.complaint.model.Complaint;

import java.util.PriorityQueue;
import java.util.List;

public class PriorityService {

    /**
     * Automatic Priority Engine Rule-Based Classifier.
     * Evaluates category, title, description, and location to determine priority.
     * Ensures safety hazards (such as exposed live electrical wire in occupied classroom) are strictly classified as CRITICAL.
     */
    public String calculatePriority(String category, String title, String description, String location) {
        String text = (title + " " + description + " " + category + " " + location).toLowerCase();

        // Rule 1: CRITICAL - Safety hazards, exposed electrical wire, live sparks, fire, major flood, classroom electrical danger
        if (text.contains("exposed wire") || text.contains("live wire") || text.contains("electrical wire") || 
            text.contains("spark") || text.contains("smoke") || text.contains("fire") || text.contains("gas leak") || 
            text.contains("flooding") || text.contains("danger") || text.contains("collapse") ||
            text.contains("electric shock") || text.contains("severe injury") || 
            (text.contains("electrical") && text.contains("classroom") && (text.contains("exposed") || text.contains("wire") || text.contains("hazard")))) {
            return "CRITICAL";
        }

        // Rule 2: HIGH - Major service disruption, water pipe burst, security gate broken, lab equipment down
        if (category != null && category.equalsIgnoreCase("Water") && (text.contains("burst") || text.contains("leak") || text.contains("overflow"))) {
            return "HIGH";
        }
        if (text.contains("broken door") || text.contains("lock") || text.contains("security") || 
            text.contains("projector") || text.contains("power outage") || text.contains("no water")) {
            return "HIGH";
        }

        // Rule 3: MEDIUM - Standard maintenance, rattling fans, broken desk, AC not working
        if (category != null && (category.equalsIgnoreCase("Classroom") || category.equalsIgnoreCase("Hostel") || category.equalsIgnoreCase("Equipment"))) {
            if (text.contains("fan") || text.contains("light") || text.contains("chair") || text.contains("desk") || text.contains("ac") || text.contains("window")) {
                return "MEDIUM";
            }
        }

        // Rule 4: LOW - Minor aesthetic, minor cleanliness, non-urgent routine upkeep
        if (category != null && category.equalsIgnoreCase("Cleaning and Maintenance")) {
            return "LOW";
        }

        // Default fallback based on category
        if (category != null) {
            switch (category) {
                case "Electrical": return "HIGH";
                case "Water": return "HIGH";
                case "Equipment": return "MEDIUM";
                case "Classroom": return "MEDIUM";
                case "Hostel": return "MEDIUM";
                case "Cleaning and Maintenance": return "LOW";
                default: return "LOW";
            }
        }

        return "LOW";
    }

    /**
     * Creates an in-memory Java PriorityQueue from a list of complaints.
     * Demonstrates data structure sorting (CRITICAL -> HIGH -> MEDIUM -> LOW with tie-breakers).
     */
    public PriorityQueue<Complaint> buildPriorityQueue(List<Complaint> complaints) {
        PriorityQueue<Complaint> pq = new PriorityQueue<>();
        if (complaints != null) {
            pq.addAll(complaints);
        }
        return pq;
    }
}
