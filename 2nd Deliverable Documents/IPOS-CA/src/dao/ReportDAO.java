package dao;

import database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportDAO
 *
 * Handles all report queries for IPOS-CA-RPT.
 * Returns raw data as lists of Object arrays — the GUI panel
 * puts these into DefaultTableModel rows directly.
 *
 * Supports: CA-26 Turnover, CA-27 Stock, CA-28 Debt
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class ReportDAO {

    // ---------------------------------------------------------------
    // CA-26: Turnover Report
    // ---------------------------------------------------------------

    /**
     * Returns all sales within a date range for the turnover report.
     * Each row: [invoice_number, sale_date, customer_name, subtotal, vat, discount, total, method]
     *
     * @param from start of period
     * @param to   end of period
     * @return list of row data arrays
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getTurnoverReport(Timestamp from, Timestamp to) throws SQLException {
        String sql = "SELECT s.invoice_number, s.sale_timestamp, "
                   + "COALESCE(CONCAT(ah.first_name, ' ', ah.last_name), s.occasional_name, 'Walk-in') AS customer, "
                   + "s.subtotal, s.vat_amount, s.discount_amount, s.total_amount, s.payment_method "
                   + "FROM sales s "
                   + "LEFT JOIN account_holders ah ON s.holder_id = ah.holder_id "
                   + "WHERE s.sale_timestamp BETWEEN ? AND ? "
                   + "ORDER BY s.sale_id";
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("invoice_number"),
                    rs.getTimestamp("sale_timestamp").toLocalDateTime().toLocalDate().toString(),
                    rs.getString("customer"),
                    "£" + rs.getBigDecimal("subtotal"),
                    "£" + rs.getBigDecimal("vat_amount"),
                    "£" + rs.getBigDecimal("discount_amount"),
                    "£" + rs.getBigDecimal("total_amount"),
                    rs.getString("payment_method")
                });
            }
        }
        return rows;
    }

    /**
     * Returns the total sales value for a period (summary line for turnover report).
     * Row: [total_sales, total_vat, total_discount, grand_total]
     *
     * @param from start of period
     * @param to   end of period
     * @return single Object array with totals
     * @throws SQLException if a database error occurs
     */
    public Object[] getTurnoverTotals(Timestamp from, Timestamp to) throws SQLException {
        String sql = "SELECT SUM(subtotal), SUM(vat_amount), SUM(discount_amount), SUM(total_amount) "
                   + "FROM sales WHERE sale_timestamp BETWEEN ? AND ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    "£" + (rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : "0.00"),
                    "£" + (rs.getBigDecimal(2) != null ? rs.getBigDecimal(2) : "0.00"),
                    "£" + (rs.getBigDecimal(3) != null ? rs.getBigDecimal(3) : "0.00"),
                    "£" + (rs.getBigDecimal(4) != null ? rs.getBigDecimal(4) : "0.00")
                };
            }
        }
        return new Object[]{"£0.00", "£0.00", "£0.00", "£0.00"};
    }

    // ---------------------------------------------------------------
    // CA-27: Stock Availability Report
    // ---------------------------------------------------------------

    /**
     * Returns all stock items for the stock availability report.
     * Each row: [description, qty_available, min_level, bulk_cost, markup_rate,
     *            retail_price_ex_vat, status]
     *
     * @param vatRate the current VAT rate (from merchant_settings)
     * @return list of row data arrays
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getStockReport(double vatRate) throws SQLException {
        String sql = "SELECT sa_item_id, description, quantity_available, min_stock_level, "
                   + "bulk_cost, markup_rate, "
                   + "ROUND(bulk_cost * (1 + markup_rate/100), 2) AS retail_ex_vat "
                   + "FROM stock_items ORDER BY stock_item_id";
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int qty    = rs.getInt("quantity_available");
                int minLvl = rs.getInt("min_stock_level");
                // Recommended min order: bring stock to 110% of min level
                // per spec: "availability should become 10% above the stock limit"
                int recommended = qty < minLvl
                    ? (int) Math.ceil(minLvl * 1.1) - qty
                    : 0;
                rows.add(new Object[]{
                    rs.getString("sa_item_id"),
                    rs.getString("description"),
                    qty,
                    minLvl,
                    recommended > 0 ? recommended : "—",
                    "£" + rs.getBigDecimal("bulk_cost"),
                    rs.getBigDecimal("markup_rate") + "%",
                    "£" + rs.getBigDecimal("retail_ex_vat"),
                    qty < minLvl ? "⚠ LOW STOCK" : "OK"
                });
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------
    // CA-28: Account Holder Debt Report
    // ---------------------------------------------------------------

    /**
     * Returns the debt summary for all account holders for a given period.
     *
     * Each row: [holder_name, opening_balance, payments_received, new_charges, closing_balance, status]
     *
     * Opening balance  = outstanding_balance at start of period (approximated as current - new charges + payments)
     * Payments received = SUM of payments in the period
     * New charges      = SUM of account sales in the period
     * Closing balance  = current outstanding_balance
     *
     * @param from start of period
     * @param to   end of period
     * @return list of row data arrays
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getDebtReport(Timestamp from, Timestamp to) throws SQLException {
        String sql =
            "SELECT CONCAT(ah.first_name, ' ', ah.last_name) AS holder_name, "
          + "ah.outstanding_balance AS closing_balance, "
          + "ah.account_status, "
          + "COALESCE(SUM(ahp.amount), 0) AS payments_received, "
          + "COALESCE(sale_totals.total_charged, 0) AS new_charges "
          + "FROM account_holders ah "
          + "LEFT JOIN account_holder_payments ahp "
          + "  ON ah.holder_id = ahp.holder_id "
          + "  AND ahp.payment_date BETWEEN ? AND ? "
          + "LEFT JOIN ("
          + "  SELECT holder_id, SUM(total_amount) AS total_charged "
          + "  FROM sales "
          + "  WHERE sale_timestamp BETWEEN ? AND ? AND payment_method = 'ACCOUNT' "
          + "  GROUP BY holder_id"
          + ") sale_totals ON ah.holder_id = sale_totals.holder_id "
          + "WHERE ah.outstanding_balance > 0 OR sale_totals.total_charged > 0 "
          + "GROUP BY ah.holder_id, ah.first_name, ah.last_name, "
          + "ah.outstanding_balance, ah.account_status, sale_totals.total_charged "
          + "ORDER BY ah.holder_id";

        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double closing  = rs.getDouble("closing_balance");
                double payments = rs.getDouble("payments_received");
                double charges  = rs.getDouble("new_charges");
                double opening  = closing - charges + payments;
                rows.add(new Object[]{
                    rs.getString("holder_name"),
                    String.format("£%.2f", opening),
                    String.format("£%.2f", payments),
                    String.format("£%.2f", charges),
                    String.format("£%.2f", closing),
                    rs.getString("account_status")
                });
            }
        }
        return rows;
    }
}
