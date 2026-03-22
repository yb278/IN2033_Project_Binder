package dao;

import database.DatabaseConnection;
import models.Order;
import models.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO
 *
 * Data Access Object for orders placed by the pharmacy with IPOS-SA (InfoPharma).
 * Maps to the {@code orders_to_sa} and {@code order_items} tables.
 *
 * Supports: CA-23, CA-29, CA-30, CA-31, CA-32
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class OrderDAO {

    // ---------------------------------------------------------------
    // CA-29: Place a new order with IPOS-SA
    // ---------------------------------------------------------------

    /**
     * Inserts a new order header and all its line items in one transaction.
     *
     * @param order the Order to persist (orderId set on return)
     * @return true if successful
     * @throws SQLException if a database error occurs
     */
    public boolean createOrder(Order order) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            // Insert order header
            String headerSql = "INSERT INTO orders_to_sa "
                    + "(ipos_sa_order_ref, placed_by, order_date, total_amount, status, payment_status) "
                    + "VALUES (?, ?, NOW(), ?, 'PENDING', 'PENDING')";
            try (PreparedStatement ps = conn.prepareStatement(
                    headerSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,     order.getIposSaOrderRef());
                ps.setInt(2,        order.getPlacedBy());
                ps.setBigDecimal(3, order.getTotalAmount());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) order.setOrderId(keys.getInt(1));
            }

            // Insert each line item
            for (OrderItem item : order.getItems()) {
                item.setOrderId(order.getOrderId());
                insertOrderItem(conn, item);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void insertOrderItem(Connection conn, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,        item.getOrderId());
            ps.setInt(2,        item.getStockItemId());
            ps.setInt(3,        item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // CA-30: Update order status
    // ---------------------------------------------------------------

    /**
     * Updates the status of an order (e.g. ACCEPTED, DISPATCHED, DELIVERED).
     *
     * @param orderId   the order to update
     * @param newStatus the new status string
     * @return true if successful
     * @throws SQLException if a database error occurs
     */
    public boolean updateStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders_to_sa SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks an order as paid and records the payment date.
     *
     * @param orderId the order to update
     * @return true if successful
     * @throws SQLException if a database error occurs
     */
    public boolean markAsPaid(int orderId) throws SQLException {
        String sql = "UPDATE orders_to_sa SET payment_status = 'PAID', payment_date = NOW() "
                   + "WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    // CA-31: View all orders
    // ---------------------------------------------------------------

    /**
     * Returns all orders placed with IPOS-SA, most recent first.
     *
     * @return list of Order objects (without line items for performance)
     * @throws SQLException if a database error occurs
     */
    public List<Order> getAllOrders() throws SQLException {
        String sql = "SELECT * FROM orders_to_sa ORDER BY order_id";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) orders.add(mapRowToOrder(rs));
        }
        return orders;
    }

    /**
     * Returns a single order with all its line items.
     *
     * @param orderId the order ID to look up
     * @return Order with items, or null if not found
     * @throws SQLException if a database error occurs
     */
    public Order findById(int orderId) throws SQLException {
        String sql = "SELECT * FROM orders_to_sa WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Order order = mapRowToOrder(rs);
                order.setItems(getOrderItems(orderId));
                return order;
            }
        }
        return null;
    }

    /**
     * Returns the line items for a given order.
     *
     * @param orderId the order ID
     * @return list of OrderItem objects
     * @throws SQLException if a database error occurs
     */
    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        String sql = "SELECT oi.*, si.description FROM order_items oi "
                   + "JOIN stock_items si ON oi.stock_item_id = si.stock_item_id "
                   + "WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new OrderItem(
                    rs.getInt("order_item_id"),
                    rs.getInt("order_id"),
                    rs.getInt("stock_item_id"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_cost")
                ));
            }
        }
        return items;
    }

    // ---------------------------------------------------------------
    // CA-32: Outstanding balance owed to InfoPharma
    // ---------------------------------------------------------------

    /**
     * Returns the total amount owed to InfoPharma across all unpaid orders.
     *
     * @return total outstanding balance
     * @throws SQLException if a database error occurs
     */
    public BigDecimal getTotalOutstandingBalance() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders_to_sa "
                   + "WHERE payment_status = 'PENDING'";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    // ---------------------------------------------------------------
    // Private helper
    // ---------------------------------------------------------------

    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("order_id"),
            rs.getString("ipos_sa_order_ref"),
            rs.getInt("placed_by"),
            rs.getTimestamp("order_date"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getTimestamp("dispatch_date"),
            rs.getTimestamp("delivery_date"),
            rs.getString("courier"),
            rs.getString("courier_ref"),
            rs.getDate("expected_delivery"),
            rs.getString("payment_status"),
            rs.getTimestamp("payment_date")
        );
    }
}
