package dao;

import database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReminderDAO
 *
 * Data Access Object for reminder records.
 * Maps to the {@code reminders} table.
 *
 * Supports: CA-11 (1st reminder), CA-12 (2nd reminder)
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class ReminderDAO {

    // ---------------------------------------------------------------
    // CA-11 / CA-12: Insert a new reminder record
    // ---------------------------------------------------------------

    /**
     * Records that a reminder has been generated for an account holder.
     *
     * @param holderId      the account holder ID
     * @param reminderType  "FIRST" or "SECOND"
     * @param paymentDueBy  the payment due date printed on the letter (today + 7 days)
     * @param amountOwed    the amount stated in the reminder
     * @param saleId        the related sale ID (can be 0 if not linked to a specific sale)
     * @return true if insert succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean createReminder(int holderId, String reminderType,
                                  Date paymentDueBy, BigDecimal amountOwed,
                                  int saleId) throws SQLException {
        String sql = "INSERT INTO reminders "
                   + "(holder_id, reminder_type, generated_at, payment_due_by, amount_owed, sale_id, sent) "
                   + "VALUES (?, ?, NOW(), ?, ?, ?, FALSE)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1,        holderId);
            ps.setString(2,     reminderType);
            ps.setDate(3,       paymentDueBy);
            ps.setBigDecimal(4, amountOwed);
            if (saleId > 0) ps.setInt(5, saleId);
            else             ps.setNull(5, Types.INTEGER);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // Mark a reminder as sent
    // ---------------------------------------------------------------

    /**
     * Marks a reminder as sent (i.e. the letter has been posted/printed).
     *
     * @param reminderId the reminder ID to mark
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean markAsSent(int reminderId) throws SQLException {
        String sql = "UPDATE reminders SET sent = TRUE WHERE reminder_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reminderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------

    /**
     * Returns all reminders for a specific account holder, most recent first.
     *
     * @param holderId the account holder ID
     * @return list of reminder rows as Object arrays:
     *         [reminderId, reminderType, generatedAt, paymentDueBy, amountOwed, sent]
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> findByHolder(int holderId) throws SQLException {
        String sql = "SELECT reminder_id, reminder_type, generated_at, payment_due_by, "
                   + "amount_owed, sent FROM reminders "
                   + "WHERE holder_id = ? ORDER BY generated_at DESC";
        List<Object[]> results = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, holderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Object[]{
                    rs.getInt("reminder_id"),
                    rs.getString("reminder_type"),
                    rs.getTimestamp("generated_at"),
                    rs.getDate("payment_due_by"),
                    rs.getBigDecimal("amount_owed"),
                    rs.getBoolean("sent")
                });
            }
        }
        return results;
    }

    /**
     * Returns all unsent reminders across all account holders.
     * Used by StatementsPanel to show what still needs to go out.
     *
     * @return list of reminder rows
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getUnsentReminders() throws SQLException {
        String sql = "SELECT r.reminder_id, r.holder_id, "
                   + "CONCAT(ah.first_name, ' ', ah.last_name) as holder_name, "
                   + "r.reminder_type, r.payment_due_by, r.amount_owed "
                   + "FROM reminders r "
                   + "JOIN account_holders ah ON r.holder_id = ah.holder_id "
                   + "WHERE r.sent = FALSE ORDER BY r.generated_at";
        List<Object[]> results = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Object[]{
                    rs.getInt("reminder_id"),
                    rs.getInt("holder_id"),
                    rs.getString("holder_name"),
                    rs.getString("reminder_type"),
                    rs.getDate("payment_due_by"),
                    rs.getBigDecimal("amount_owed")
                });
            }
        }
        return results;
    }
}
