package dao;

import database.DatabaseConnection;
import models.Sale;
import models.Sale.PaymentMethod;
import models.SaleItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SalesDAO
 *
 * Data Access Object for all database operations on the {@code sales} and
 * {@code sale_items} tables.
 *
 * Supports use cases:
 *   CA-13 Record Sale
 *   CA-14 Accept Payment
 *   CA-15 Process Cash Payment
 *   CA-16 Process Card Payment
 *   CA-17 Generate Receipt/Invoice (invoice number generation here)
 *   CA-26 Turnover Report data
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class SalesDAO {

    // ---------------------------------------------------------------
    // CA-13 / CA-14 / CA-15 / CA-16: Record a complete sale
    // ---------------------------------------------------------------

    /**
     * Persists a completed sale and all its line items in a single transaction.
     * Also decrements stock quantities and (for account-holder sales) increases
     * their outstanding balance.
     *
     * @param sale    the Sale to persist (saleId and invoiceNumber set on return)
     * @param stockDAO used to deduct stock quantities
     * @param ahDAO   used to update account-holder balance if payment is ACCOUNT
     * @return true if the sale was recorded successfully
     * @throws SQLException if any database operation fails (full rollback on failure)
     */
    public boolean recordSale(Sale sale, StockDAO stockDAO, AccountHolderDAO ahDAO)
            throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false); // begin transaction
        try {
            // 1. Generate invoice number (e.g. INV-000001)
            String invoiceNumber = generateInvoiceNumber(conn);
            sale.setInvoiceNumber(invoiceNumber);

            // 2. Insert the sale header
            int saleId = insertSaleHeader(conn, sale);
            sale.setSaleId(saleId);

            // 3. Insert each line item and deduct stock
            for (SaleItem item : sale.getItems()) {
                item.setSaleId(saleId);
                insertSaleItem(conn, item);
                stockDAO.decreaseStock(item.getStockItemId(), item.getQuantity());
            }

            // 4. If payment method is ACCOUNT, add to the holder's outstanding balance
            if (sale.getPaymentMethod() == PaymentMethod.ACCOUNT && sale.getHolderId() > 0) {
                String balanceSql = "UPDATE account_holders SET outstanding_balance = "
                                  + "outstanding_balance + ? WHERE holder_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(balanceSql)) {
                    ps.setBigDecimal(1, sale.getTotalAmount());
                    ps.setInt(2, sale.getHolderId());
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            conn.rollback(); // roll back entire transaction on any failure
            throw e;         // re-throw for the caller to handle
        } finally {
            conn.setAutoCommit(true); // restore auto-commit
        }
    }

    // ---------------------------------------------------------------
    // Lookup: find sales for reports (CA-26 Turnover Report)
    // ---------------------------------------------------------------

    /**
     * Returns all sales within a given date range.
     * Used by CA-26 (Generate Turnover Report).
     *
     * @param from start of period (inclusive)
     * @param to   end of period (inclusive)
     * @return list of Sale objects (without line items for performance)
     * @throws SQLException if a database error occurs
     */
    public List<Sale> getSalesInPeriod(Timestamp from, Timestamp to) throws SQLException {
        String sql = "SELECT * FROM sales WHERE sale_timestamp BETWEEN ? AND ? "
                   + "ORDER BY sale_id";
        List<Sale> sales = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) sales.add(mapRowToSale(rs));
        }
        return sales;
    }

    /**
     * Returns all sales for a specific account holder.
     * Used when viewing account history (CA-06).
     *
     * @param holderId the account holder ID
     * @return list of Sale objects for this account holder
     * @throws SQLException if a database error occurs
     */
    public List<Sale> getSalesByAccountHolder(int holderId) throws SQLException {
        String sql = "SELECT * FROM sales WHERE holder_id = ? ORDER BY sale_id";
        List<Sale> sales = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, holderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) sales.add(mapRowToSale(rs));
        }
        return sales;
    }

    /**
     * Returns a single sale with all its line items, looked up by invoice number.
     * Used when printing receipts (CA-17).
     *
     * @param invoiceNumber the invoice reference
     * @return the Sale with line items, or null if not found
     * @throws SQLException if a database error occurs
     */
    public Sale findByInvoiceNumber(String invoiceNumber) throws SQLException {
        String sql = "SELECT * FROM sales WHERE invoice_number = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Sale sale = mapRowToSale(rs);
                sale.setItems(getSaleItems(sale.getSaleId()));
                return sale;
            }
        }
        return null;
    }

    /**
     * Returns the line items for a given sale.
     *
     * @param saleId the sale ID
     * @return list of SaleItem objects
     * @throws SQLException if a database error occurs
     */
    public List<SaleItem> getSaleItems(int saleId) throws SQLException {
        String sql = "SELECT si.*, st.description FROM sale_items si "
                   + "JOIN stock_items st ON si.stock_item_id = st.stock_item_id "
                   + "WHERE si.sale_id = ?";
        List<SaleItem> items = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new SaleItem(
                    rs.getInt("sale_item_id"),
                    rs.getInt("sale_id"),
                    rs.getInt("stock_item_id"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price"),
                    rs.getBigDecimal("line_total")
                ));
            }
        }
        return items;
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Generates the next sequential invoice number within a transaction.
     * Format: INV-NNNNNN
     */
    private String generateInvoiceNumber(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1) + 1;
                return String.format("INV-%06d", count);
            }
        }
        return "INV-000001";
    }

    /** Inserts the sale header row and returns the generated saleId */
    private int insertSaleHeader(Connection conn, Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (served_by, holder_id, occasional_name, sale_timestamp, "
                   + "subtotal, vat_amount, discount_amount, total_amount, payment_method, "
                   + "payment_received, change_given, card_type, card_first_four, "
                   + "card_last_four, card_expiry, invoice_number) "
                   + "VALUES (?,?,?,NOW(),?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sale.getServedBy());
            // Store NULL for holderId if it's an occasional customer
            if (sale.getHolderId() > 0) ps.setInt(2, sale.getHolderId());
            else                         ps.setNull(2, Types.INTEGER);
            ps.setString(3,      sale.getOccasionalName());
            ps.setBigDecimal(4,  sale.getSubtotal());
            ps.setBigDecimal(5,  sale.getVatAmount());
            ps.setBigDecimal(6,  sale.getDiscountAmount());
            ps.setBigDecimal(7,  sale.getTotalAmount());
            ps.setString(8,      sale.getPaymentMethod().name());
            ps.setBigDecimal(9,  sale.getPaymentReceived());
            ps.setBigDecimal(10, sale.getChangeGiven());
            ps.setString(11,     sale.getCardType());
            ps.setString(12,     sale.getCardFirstFour());
            ps.setString(13,     sale.getCardLastFour());
            ps.setString(14,     sale.getCardExpiry());
            ps.setString(15,     sale.getInvoiceNumber());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new SQLException("Failed to obtain generated sale ID.");
    }

    /** Inserts a single sale line item */
    private void insertSaleItem(Connection conn, SaleItem item) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) "
                   + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,        item.getSaleId());
            ps.setInt(2,        item.getStockItemId());
            ps.setInt(3,        item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.executeUpdate();
        }
    }

    /** Maps a ResultSet row to a Sale object (without line items) */
    private Sale mapRowToSale(ResultSet rs) throws SQLException {
        return new Sale(
            rs.getInt("sale_id"),
            rs.getInt("served_by"),
            rs.getInt("holder_id"),
            rs.getString("occasional_name"),
            rs.getTimestamp("sale_timestamp"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("vat_amount"),
            rs.getBigDecimal("discount_amount"),
            rs.getBigDecimal("total_amount"),
            PaymentMethod.valueOf(rs.getString("payment_method")),
            rs.getBigDecimal("payment_received"),
            rs.getBigDecimal("change_given"),
            rs.getString("card_type"),
            rs.getString("card_first_four"),
            rs.getString("card_last_four"),
            rs.getString("card_expiry"),
            rs.getString("invoice_number")
        );
    }
}
