package com.campus.complaint.service;

import com.campus.complaint.dao.UserDAO;
import com.campus.complaint.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthenticationService {
    private UserDAO userDAO = new UserDAO();

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User user = userDAO.findByUsername(username.trim());
        if (user != null) {
            // For default sample user login convenience or hashed verification
            if (user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$")) {
                if (BCrypt.checkpw(password, user.getPasswordHash())) {
                    return user;
                }
            } else if (password.equals(user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }
}
