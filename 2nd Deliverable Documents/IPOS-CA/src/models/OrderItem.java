package models;

import java.math.BigDecimal;

/**
 * OrderItem
 *
 * A single line item within an order placed with IPOS-SA (InfoPharma).
 * Maps to the {@code order_items} table in the ipos_ca database.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class OrderItem {

    private int        orderItemId;
    private int        orderId;
    private int        stockItemId;
    private String     description;
    private int        quantity;
    private BigDecimal unitCost;

    /** Full constructor (loading from DB) */
    public OrderItem(int orderItemId, int orderId, int stockItemId,
                     String description, int quantity, BigDecimal unitCost) {
        this.orderItemId = orderItemId;
        this.orderId     = orderId;
        this.stockItemId = stockItemId;
        this.description = description;
        this.quantity    = quantity;
        this.unitCost    = unitCost;
    }

    /** Constructor for creating a new order item */
    public OrderItem(int stockItemId, int quantity, BigDecimal unitCost) {
        this.stockItemId = stockItemId;
        this.quantity    = quantity;
        this.unitCost    = unitCost;
    }

    public int        getOrderItemId()             { return orderItemId; }
    public int        getOrderId()                 { return orderId; }
    public void       setOrderId(int id)           { this.orderId = id; }
    public int        getStockItemId()             { return stockItemId; }
    public String     getDescription()             { return description; }
    public int        getQuantity()                { return quantity; }
    public BigDecimal getUnitCost()                { return unitCost; }
    public BigDecimal getLineTotal() {
        return unitCost.multiply(new BigDecimal(quantity));
    }

    @Override
    public String toString() {
        return "OrderItem{stockId=" + stockItemId + ", desc='" + description
               + "', qty=" + quantity + ", unitCost=£" + unitCost + "}";
    }
}
