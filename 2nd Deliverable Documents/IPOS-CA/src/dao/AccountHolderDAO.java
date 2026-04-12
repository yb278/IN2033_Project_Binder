package dao;

import database.DatabaseConnection;
import models.AccountHolder;
import models.AccountHolder.AccountStatus;
import models.AccountHolder.ReminderStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountHolderDAO
 *
 * Data Access Object for all database operations on the {@code account_holders} table.
 *
 * Supports use cases:
 *   CA-03 Create account holder
 *   CA-04 Remove (deactivate) account holder  [note: not hard delete – use status approach]
 *   CA-05 Update account holder details
 *   CA-06 View account status
 *   CA-09 Update account status (automated and manual)
 *   CA-10 Record payment (see also AccountHolderPaymentDAO)
 *   CA-11 / CA-12 Reminder state management
 *   CA-33 Set credit limit
 *   CA-34 Apply discount plan
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class AccountHolderDAO {

    // ---------------------------------------------------------------
    // CA-03: Create account holder
    // ---------------------------------------------------------------

    /**
     * Inserts a new account holder record into the database.
     *
     * @param holder the AccountHolder to persist (holderId set on return)
     * @return true if insert succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean createAccountHolder(AccountHolder holder) throws SQLException {
        String sql = "INSERT INTO account_holders "
                   + "(first_name, last_name, date_of_birth, email, phone, "
                   + " address_line1, address_line2, city, postcode, "
                   + " account_status, credit_limit, outstanding_balance, discount_plan_id, "
                   + " monthly_order_total, status_1st_reminder, status_2nd_reminder) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  holder.getFirstName());
            ps.setString(2,  holder.getLastName());
            ps.setDate(3,    holder.getDateOfBirth());
            ps.setString(4,  holder.getEmail());
            ps.setString(5,  holder.getPhone());
            ps.setString(6,  holder.getAddressLine1());
            ps.setString(7,  holder.getAddressLine2());
            ps.setString(8,  holder.getCity());
            ps.setString(9,  holder.getPostcode());
            ps.setString(10, holder.getAccountStatus().name());
            ps.setBigDecimal(11, holder.getCreditLimit());
            ps.setBigDecimal(12, holder.getOutstandingBalance());
            ps.setInt(13, holder.getDiscountPlanId());
            ps.setBigDecimal(14, holder.getMonthlyOrderTotal());
            ps.setString(15, holder.getStatus1stReminder().name());
            ps.setString(16, holder.getStatus2ndReminder().name());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) holder.setHolderId(keys.getInt(1));
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // CA-05: Update account holder details
    // ---------------------------------------------------------------

    /**
     * Updates the personal contact details of an existing account holder.
     *
     * @param holder the AccountHolder with updated details
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateAccountHolder(AccountHolder holder) throws SQLException {
        String sql = "UPDATE account_holders SET "
                   + "first_name=?, last_name=?, date_of_birth=?, email=?, phone=?, "
                   + "address_line1=?, address_line2=?, city=?, postcode=? "
                   + "WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, holder.getFirstName());
            ps.setString(2, holder.getLastName());
            ps.setDate(3,   holder.getDateOfBirth());
            ps.setString(4, holder.getEmail());
            ps.setString(5, holder.getPhone());
            ps.setString(6, holder.getAddressLine1());
            ps.setString(7, holder.getAddressLine2());
            ps.setString(8, holder.getCity());
            ps.setString(9, holder.getPostcode());
            ps.setInt(10,   holder.getHolderId());
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-09: Update account status
    // ---------------------------------------------------------------

    /**
     * Updates the account status of an account holder.
     * Called both automatically (by the scheduled status-check logic)
     * and manually (by a Manager to restore an IN_DEFAULT account).
     *
     * @param holderId the account holder ID
     * @param newStatus the new status to apply
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateAccountStatus(int holderId, AccountStatus newStatus) throws SQLException {
        String sql = "UPDATE account_holders SET account_status=?, last_status_change=NOW() WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, holderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-11 / CA-12: Reminder state updates
    // ---------------------------------------------------------------

    /**
     * Updates the reminder state fields for an account holder.
     * Implements the synchronisation mechanism from Student Brief §8.2 pseudo-code.
     *
     * @param holderId          the account holder ID
     * @param status1st         new status_1st_reminder value
     * @param status2nd         new status_2nd_reminder value
     * @param date1st           new date_1st_reminder (may be null)
     * @param date2nd           new date_2nd_reminder (may be null)
     * @throws SQLException if a database error occurs
     */
    public void updateReminderState(int holderId,
                                    ReminderStatus status1st, ReminderStatus status2nd,
                                    Date date1st, Date date2nd) throws SQLException {
        String sql = "UPDATE account_holders SET "
                   + "status_1st_reminder=?, status_2nd_reminder=?, "
                   + "date_1st_reminder=?, date_2nd_reminder=? "
                   + "WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status1st.name());
            ps.setString(2, status2nd.name());
            ps.setDate(3, date1st);
            ps.setDate(4, date2nd);
            ps.setInt(5, holderId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // CA-10: Update outstanding balance after payment
    // ---------------------------------------------------------------

    /**
     * Reduces the outstanding balance of an account holder when a payment is received.
     * Also resets reminder state flags as per Student Brief §8.2.
     *
     * @param holderId      the account holder ID
     * @param amountPaid    the amount paid
     * @throws SQLException if a database error occurs
     */
    public void recordPayment(int holderId, BigDecimal amountPaid) throws SQLException {
        // Reduce balance
        String balanceSql = "UPDATE account_holders SET outstanding_balance = "
                          + "GREATEST(0, outstanding_balance - ?) WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(balanceSql)) {
            ps.setBigDecimal(1, amountPaid);
            ps.setInt(2, holderId);
            ps.executeUpdate();
        }

        // Re-check if the updated balance is zero; if so, restore NORMAL status and clear reminders
        AccountHolder holder = findById(holderId);
        if (holder != null && holder.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            if (holder.getAccountStatus() != AccountStatus.IN_DEFAULT) {
                // Automatically restore NORMAL if not IN_DEFAULT (IN_DEFAULT needs manual restore)
                updateAccountStatus(holderId, AccountStatus.NORMAL);
                updateReminderState(holderId,
                        ReminderStatus.no_need, ReminderStatus.no_need, null, null);
            }
        }
    }

    /**
     * Returns the full payment history for an account holder,
     * ordered most recent first.
     * Each row: [paymentId, amount, paymentDate, notes]
     */
    public List<Object[]> getPaymentHistory(int holderId) throws SQLException {
        String sql = "SELECT payment_id, amount, payment_date, notes "
                   + "FROM account_holder_payments WHERE holder_id = ? "
                   + "ORDER BY payment_date DESC";
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, holderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("payment_id"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("payment_date").toLocalDateTime()
                        .toLocalDate().toString(),
                    rs.getString("notes") != null ? rs.getString("notes") : ""
                });
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------
    // CA-33: Set credit limit
    // ---------------------------------------------------------------

    /**
     * Updates the credit limit for an account holder.
     *
     * @param holderId    the account holder ID
     * @param creditLimit the new credit limit
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean setCreditLimit(int holderId, BigDecimal creditLimit) throws SQLException {
        String sql = "UPDATE account_holders SET credit_limit=? WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setBigDecimal(1, creditLimit);
            ps.setInt(2, holderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-34: Apply discount plan
    // ---------------------------------------------------------------

    /**
     * Assigns a discount plan to an account holder.
     *
     * @param holderId      the account holder ID
     * @param discountPlanId the ID of the discount plan to apply
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean applyDiscountPlan(int holderId, int discountPlanId) throws SQLException {
        String sql = "UPDATE account_holders SET discount_plan_id=? WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, discountPlanId);
            ps.setInt(2, holderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------

    /**
     * Finds an account holder by their ID.
     *
     * @param holderId the holder ID to search for
     * @return the AccountHolder, or null if not found
     * @throws SQLException if a database error occurs
     */
    public AccountHolder findById(int holderId) throws SQLException {
        String sql = "SELECT * FROM account_holders WHERE holder_id=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, holderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowToAccountHolder(rs);
        }
        return null;
    }

    /**
     * Searches for account holders by name (partial match, case-insensitive).
     *
     * @param searchTerm partial first or last name
     * @return list of matching AccountHolder objects
     * @throws SQLException if a database error occurs
     */
    public List<AccountHolder> searchByName(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM account_holders WHERE "
                   + "LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? "
                   + "ORDER BY holder_id";
        List<AccountHolder> results = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(mapRowToAccountHolder(rs));
        }
        return results;
    }

    /**
     * Returns all account holders whose reminder state requires action.
     * Used by the reminder generation workflow (CA-11, CA-12).
     *
     * @return list of AccountHolder objects with 'due' reminder status
     * @throws SQLException if a database error occurs
     */
    public List<AccountHolder> findWithDueReminders() throws SQLException {
        String sql = "SELECT * FROM account_holders WHERE "
                   + "status_1st_reminder = 'due' OR status_2nd_reminder = 'due'";
        List<AccountHolder> results = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(mapRowToAccountHolder(rs));
        }
        return results;
    }

    /**
     * Returns all account holders – used for the account holder debt report (CA-28).
     *
     * @return list of all AccountHolder objects
     * @throws SQLException if a database error occurs
     */
    public List<AccountHolder> getAllAccountHolders() throws SQLException {
        String sql = "SELECT * FROM account_holders ORDER BY holder_id";
        List<AccountHolder> results = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(mapRowToAccountHolder(rs));
        }
        return results;
    }

    /**
     * Permanently deletes an account holder and all their associated records
     * (payments, reminders) in a single transaction.
     * Only safe to use when the holder has no outstanding balance and
     * no linked sales records (sales reference holder_id).
     *
     * @param holderId the ID of the account holder to delete
     * @throws SQLException if a database error occurs or FK constraints prevent deletion
     */
    public void deleteAccountHolder(int holderId) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            // Delete child records first to satisfy FK constraints
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM reminders WHERE holder_id = ?")) {
                ps.setInt(1, holderId); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM account_holder_payments WHERE holder_id = ?")) {
                ps.setInt(1, holderId); ps.executeUpdate();
            }
            // Delete the account holder record itself
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM account_holders WHERE holder_id = ?")) {
                ps.setInt(1, holderId); ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ---------------------------------------------------------------
    // Private helper: map a ResultSet row to an AccountHolder object
    // ---------------------------------------------------------------

    private AccountHolder mapRowToAccountHolder(ResultSet rs) throws SQLException {
        return new AccountHolder(
            rs.getInt("holder_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getDate("date_of_birth"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("address_line1"),
            rs.getString("address_line2"),
            rs.getString("city"),
            rs.getString("postcode"),
            AccountStatus.valueOf(rs.getString("account_status")),
            rs.getBigDecimal("credit_limit"),
            rs.getBigDecimal("outstanding_balance"),
            rs.getInt("discount_plan_id"),
            rs.getBigDecimal("monthly_order_total"),
            ReminderStatus.valueOf(rs.getString("status_1st_reminder")),
            ReminderStatus.valueOf(rs.getString("status_2nd_reminder")),
            rs.getDate("date_1st_reminder"),
            rs.getDate("date_2nd_reminder"),
            rs.getDate("payment_due_date")
        );
    }
}
