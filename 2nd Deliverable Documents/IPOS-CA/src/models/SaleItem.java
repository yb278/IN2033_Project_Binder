package models;

import java.math.BigDecimal;

/**
 * SaleItem
 *
 * Represents a single line item within a sale transaction.
 * Maps to the {@code sale_items} table in the ipos_ca database.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class SaleItem {

    private int        saleItemId;
    private int        saleId;
    private int        stockItemId;
    private String     description;
    private int        quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    /** Full constructor (loading from DB) */
    public SaleItem(int saleItemId, int saleId, int stockItemId, String description,
                    int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleItemId  = saleItemId;
        this.saleId      = saleId;
        this.stockItemId = stockItemId;
        this.description = description;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.lineTotal   = lineTotal;
    }

    /** Constructor for creating a new line item */
    public SaleItem(int saleId, int stockItemId, String description,
                    int quantity, BigDecimal unitPrice) {
        this.saleId      = saleId;
        this.stockItemId = stockItemId;
        this.description = description;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.lineTotal   = unitPrice.multiply(new BigDecimal(quantity));
    }

    public int        getSaleItemId()              { return saleItemId; }
    public void       setSaleItemId(int id)        { this.saleItemId = id; }
    public int        getSaleId()                  { return saleId; }
    public void       setSaleId(int id)            { this.saleId = id; }
    public int        getStockItemId()             { return stockItemId; }
    public void       setStockItemId(int id)       { this.stockItemId = id; }
    public String     getDescription()             { return description; }
    public void       setDescription(String d)     { this.description = d; }
    public int        getQuantity()                { return quantity; }
    public void       setQuantity(int q)           { this.quantity = q; }
    public BigDecimal getUnitPrice()               { return unitPrice; }
    public void       setUnitPrice(BigDecimal up)  { this.unitPrice = up; }
    public BigDecimal getLineTotal()               { return lineTotal; }
    public void       setLineTotal(BigDecimal lt)  { this.lineTotal = lt; }

    @Override
    public String toString() {
        return "SaleItem{stockId=" + stockItemId + ", desc='" + description
               + "', qty=" + quantity + ", unitPrice=£" + unitPrice
               + ", lineTotal=£" + lineTotal + "}";
    }
}
