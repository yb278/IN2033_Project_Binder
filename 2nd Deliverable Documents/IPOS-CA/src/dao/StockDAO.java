package dao;

import database.DatabaseConnection;
import models.StockItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StockDAO
 *
 * Data Access Object for all database operations on the {@code stock_items} table.
 *
 * Supports use cases:
 *   CA-18 Maintain Stock Records
 *   CA-19 View Stock Availability
 *   CA-20 Check If Stock Low
 *   CA-22 Update Retail Mark-up Rate
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class StockDAO {

    // ---------------------------------------------------------------
    // CA-18: Add a new stock item to local inventory
    // ---------------------------------------------------------------

    /**
     * Inserts a new stock item into the local pharmacy inventory.
     * Typically called when a new product is received from InfoPharma (IPOS-SA).
     *
     * @param item the StockItem to persist (stockItemId set on return)
     * @return true if insert succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean addStockItem(StockItem item) throws SQLException {
        String sql = "INSERT INTO stock_items "
                   + "(sa_item_id, description, package_type, unit, units_per_pack, "
                   + " bulk_cost, markup_rate, quantity_available, min_stock_level) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,     item.getSaItemId());
            ps.setString(2,     item.getDescription());
            ps.setString(3,     item.getPackageType());
            ps.setString(4,     item.getUnit());
            ps.setInt(5,        item.getUnitsPerPack());
            ps.setBigDecimal(6, item.getBulkCost());
            ps.setBigDecimal(7, item.getMarkupRate());
            ps.setInt(8,        item.getQuantityAvailable());
            ps.setInt(9,        item.getMinStockLevel());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) item.setStockItemId(keys.getInt(1));
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // CA-18: Update stock quantity (after order received from IPOS-SA)
    // ---------------------------------------------------------------

    /**
     * Updates the available quantity of a stock item.
     * Called after an order from IPOS-SA is delivered and booked in.
     *
     * @param stockItemId      the stock item to update
     * @param quantityToAdd    the number of packs received
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean increaseStock(int stockItemId, int quantityToAdd) throws SQLException {
        String sql = "UPDATE stock_items SET quantity_available = quantity_available + ? "
                   + "WHERE stock_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, quantityToAdd);
            ps.setInt(2, stockItemId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Decreases the available quantity of a stock item after a sale.
     * Rejects the update if it would result in negative stock.
     *
     * @param stockItemId   the stock item to update
     * @param quantitySold  the number of packs sold
     * @return true if update succeeded
     * @throws SQLException if a database error occurs or insufficient stock
     */
    public boolean decreaseStock(int stockItemId, int quantitySold) throws SQLException {
        // Check current stock first
        StockItem item = findById(stockItemId);
        if (item == null || item.getQuantityAvailable() < quantitySold) {
            throw new SQLException("Insufficient stock for item ID " + stockItemId);
        }
        String sql = "UPDATE stock_items SET quantity_available = quantity_available - ? "
                   + "WHERE stock_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, quantitySold);
            ps.setInt(2, stockItemId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-22: Update Retail Mark-up Rate
    // ---------------------------------------------------------------

    /**
     * Updates the retail markup rate for a specific stock item.
     *
     * @param stockItemId  the stock item to update
     * @param markupRate   the new markup percentage (e.g. 20.00 = 20%)
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateMarkupRate(int stockItemId, BigDecimal markupRate) throws SQLException {
        String sql = "UPDATE stock_items SET markup_rate = ? WHERE stock_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setBigDecimal(1, markupRate);
            ps.setInt(2, stockItemId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the minimum stock level threshold for a stock item.
     * Used to configure the low-stock alert trigger (CA-20).
     *
     * @param stockItemId    the stock item to update
     * @param minStockLevel  the new minimum stock level
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateMinStockLevel(int stockItemId, int minStockLevel) throws SQLException {
        String sql = "UPDATE stock_items SET min_stock_level = ? WHERE stock_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, minStockLevel);
            ps.setInt(2, stockItemId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-19: View all stock
    // ---------------------------------------------------------------

    /**
     * Returns all stock items for display.
     * Used by CA-19 (View Stock Availability).
     *
     * @return list of all StockItem objects
     * @throws SQLException if a database error occurs
     */
    public List<StockItem> getAllStockItems() throws SQLException {
        String sql = "SELECT * FROM stock_items ORDER BY stock_item_id";
        List<StockItem> items = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) items.add(mapRowToStockItem(rs));
        }
        return items;
    }

    // ---------------------------------------------------------------
    // CA-20: Check If Stock Low
    // ---------------------------------------------------------------

    /**
     * Returns stock items where quantity is at or below the minimum stock level.
     * Used to generate low-stock alerts and the stock availability report (CA-27).
     *
     * @return list of low-stock StockItem objects
     * @throws SQLException if a database error occurs
     */
    public List<StockItem> getLowStockItems() throws SQLException {
        String sql = "SELECT * FROM stock_items WHERE quantity_available <= min_stock_level "
                   + "ORDER BY stock_item_id";
        List<StockItem> items = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) items.add(mapRowToStockItem(rs));
        }
        return items;
    }

    // ---------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------

    /**
     * Finds a stock item by its local ID.
     *
     * @param stockItemId the local stock item ID
     * @return the StockItem, or null if not found
     * @throws SQLException if a database error occurs
     */
    public StockItem findById(int stockItemId) throws SQLException {
        String sql = "SELECT * FROM stock_items WHERE stock_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, stockItemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowToStockItem(rs);
        }
        return null;
    }

    /**
     * Finds a stock item by its IPOS-SA catalogue ID (cross-system reference).
     *
     * @param saItemId the IPOS-SA item ID (e.g. "100 00001")
     * @return the StockItem, or null if not found
     * @throws SQLException if a database error occurs
     */
    public StockItem findBySaItemId(String saItemId) throws SQLException {
        String sql = "SELECT * FROM stock_items WHERE sa_item_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, saItemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowToStockItem(rs);
        }
        return null;
    }

    /**
     * Searches for stock items by description (partial match, case-insensitive).
     *
     * @param searchTerm the keyword to search for
     * @return list of matching StockItem objects
     * @throws SQLException if a database error occurs
     */
    public List<StockItem> searchByDescription(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM stock_items WHERE LOWER(description) LIKE ? ORDER BY description";
        List<StockItem> items = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + searchTerm.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) items.add(mapRowToStockItem(rs));
        }
        return items;
    }

    // ---------------------------------------------------------------
    // Private helper: map a ResultSet row to a StockItem object
    // ---------------------------------------------------------------

    private StockItem mapRowToStockItem(ResultSet rs) throws SQLException {
        return new StockItem(
            rs.getInt("stock_item_id"),
            rs.getString("sa_item_id"),
            rs.getString("description"),
            rs.getString("package_type"),
            rs.getString("unit"),
            rs.getInt("units_per_pack"),
            rs.getBigDecimal("bulk_cost"),
            rs.getBigDecimal("markup_rate"),
            rs.getInt("quantity_available"),
            rs.getInt("min_stock_level")
        );
    }
}
