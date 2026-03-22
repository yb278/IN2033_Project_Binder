package dao;

import database.DatabaseConnection;
import models.User;
import models.User.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO
 *
 * Data Access Object for all database operations on the {@code users} table.
 * Supports: CA-01 (Login), CA-03 (Create User), CA-04 (Remove User), CA-08 (Assign Role)
 *
 * NOTE: In production, passwords must be hashed (e.g. BCrypt).
 * For the prototype a plain-text comparison is used for simplicity.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class UserDAO {

    // ---------------------------------------------------------------
    // CA-01: Login / Authentication
    // ---------------------------------------------------------------

    /**
     * Authenticates a user by username and password.
     * Returns the User object if credentials are valid and account is active,
     * or null if authentication fails.
     *
     * @param username the entered username
     * @param password the entered password (plain text in prototype)
     * @return User on success, null on failure
     * @throws SQLException if a database error occurs
     */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND is_active = TRUE";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password); // prototype: plain text; production: hash before comparing
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        }
        return null; // authentication failed
    }

    // ---------------------------------------------------------------
    // CA-03: Create User Account
    // ---------------------------------------------------------------

    /**
     * Creates a new staff user account in the database.
     * Only callable by ADMIN users (enforced in the service/controller layer).
     *
     * @param user the User object to persist (userId will be set on return)
     * @return true if the insert succeeded
     * @throws SQLException if a database error occurs or username already exists
     */
    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, first_name, last_name, role, is_active) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isActive());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    user.setUserId(keys.getInt(1)); // set the auto-generated ID back onto the object
                }
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // CA-04: Remove User Account (deactivate rather than hard-delete)
    // ---------------------------------------------------------------

    /**
     * Deactivates a user account (soft delete).
     * Preserves data integrity for audit trails.
     *
     * @param userId the ID of the user to deactivate
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean deactivateUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = FALSE WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-08: Assign / Update Role
    // ---------------------------------------------------------------

    /**
     * Updates the role assigned to a user account.
     *
     * @param userId the ID of the user to update
     * @param newRole the new role to assign
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateUserRole(int userId, Role newRole) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newRole.name());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------

    /**
     * Finds a user by their user ID.
     *
     * @param userId the ID to search for
     * @return the User, or null if not found
     * @throws SQLException if a database error occurs
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowToUser(rs);
        }
        return null;
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the User, or null if not found
     * @throws SQLException if a database error occurs
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowToUser(rs);
        }
        return null;
    }

    /**
     * Returns all active user accounts, ordered by ID.
     *
     * @return list of active User objects
     * @throws SQLException if a database error occurs
     */
    public List<User> getAllActiveUsers() throws SQLException {
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapRowToUser(rs));
        }
        return users;
    }

    /**
     * Returns ALL user accounts including inactive ones, ordered by ID.
     * Used by UserManagementPanel so admins can reactivate deactivated accounts.
     *
     * @return list of all User objects
     * @throws SQLException if a database error occurs
     */
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapRowToUser(rs));
        }
        return users;
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * @param userId the ID of the user to reactivate
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean reactivateUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the password for a user.
     *
     * @param userId      the ID of the user
     * @param newPassword the new password (plain text in prototype)
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // Private helper: map a ResultSet row to a User object
    // ---------------------------------------------------------------

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            Role.valueOf(rs.getString("role")),
            rs.getBoolean("is_active")
        );
    }
}
