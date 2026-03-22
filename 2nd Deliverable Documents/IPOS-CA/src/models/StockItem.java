package models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * StockItem
 *
 * Represents a pharmaceutical product in the pharmacy's local stock.
 * Maps to the {@code stock_items} table in the ipos_ca database.
 *
 * The retail price is calculated from the bulk cost from InfoPharma
 * plus the configured markup rate (CA-22).
 * VAT is applied on top at the global rate stored in merchant_settings (CA-21).
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class StockItem {

    private int        stockItemId;
    private String     saItemId;          // corresponding ID in IPOS-SA catalogue
    private String     description;
    private String     packageType;
    private String     unit;
    private int        unitsPerPack;
    private BigDecimal bulkCost;          // cost price from InfoPharma
    private BigDecimal markupRate;        // retail markup percentage (e.g. 20.00 = 20%)
    private int        quantityAvailable;
    private int        minStockLevel;     // low-stock threshold for alerts (CA-20)

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Full constructor (loading from DB) */
    public StockItem(int stockItemId, String saItemId, String description,
                     String packageType, String unit, int unitsPerPack,
                     BigDecimal bulkCost, BigDecimal markupRate,
                     int quantityAvailable, int minStockLevel) {
        this.stockItemId       = stockItemId;
        this.saItemId          = saItemId;
        this.description       = description;
        this.packageType       = packageType;
        this.unit              = unit;
        this.unitsPerPack      = unitsPerPack;
        this.bulkCost          = bulkCost;
        this.markupRate        = markupRate;
        this.quantityAvailable = quantityAvailable;
        this.minStockLevel     = minStockLevel;
    }

    /** Constructor for adding a new stock item */
    public StockItem(String saItemId, String description, String packageType,
                     String unit, int unitsPerPack, BigDecimal bulkCost,
                     BigDecimal markupRate, int minStockLevel) {
        this(0, saItemId, description, packageType, unit, unitsPerPack,
             bulkCost, markupRate, 0, minStockLevel);
    }

    // ---------------------------------------------------------------
    // Business logic helpers
    // ---------------------------------------------------------------

    /**
     * Calculates the retail price (before VAT) by applying the markup rate.
     * retailPrice = bulkCost * (1 + markupRate / 100)
     *
     * @return retail price per pack before VAT, rounded to 2 decimal places
     */
    public BigDecimal getRetailPriceExVAT() {
        BigDecimal multiplier = BigDecimal.ONE.add(
                markupRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return bulkCost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the retail price including VAT.
     *
     * @param vatRate the VAT percentage (e.g. 20.00 for 20%), from merchant_settings
     * @return retail price per pack including VAT, rounded to 2 decimal places
     */
    public BigDecimal getRetailPriceIncVAT(BigDecimal vatRate) {
        BigDecimal vatMultiplier = BigDecimal.ONE.add(
                vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return getRetailPriceExVAT().multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns true if the current quantity is at or below the minimum stock level.
     * Used by CA-20 (Check If Stock Low) to flag items needing reordering.
     */
    public boolean isLowStock() {
        return quantityAvailable <= minStockLevel;
    }

    /**
     * Reduces stock by the given quantity after a sale.
     * Throws IllegalArgumentException if insufficient stock.
     *
     * @param qty quantity sold
     */
    public void deductStock(int qty) {
        if (qty > quantityAvailable) {
            throw new IllegalArgumentException(
                "Insufficient stock for '" + description + "'. Available: "
                + quantityAvailable + ", requested: " + qty);
        }
        this.quantityAvailable -= qty;
    }

    /**
     * Increases stock after an order from IPOS-SA is received.
     *
     * @param qty quantity received
     */
    public void addStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }
        this.quantityAvailable += qty;
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------

    public int        getStockItemId()                       { return stockItemId; }
    public void       setStockItemId(int id)                 { this.stockItemId = id; }

    public String     getSaItemId()                          { return saItemId; }
    public void       setSaItemId(String id)                 { this.saItemId = id; }

    public String     getDescription()                       { return description; }
    public void       setDescription(String d)               { this.description = d; }

    public String     getPackageType()                       { return packageType; }
    public void       setPackageType(String pt)              { this.packageType = pt; }

    public String     getUnit()                              { return unit; }
    public void       setUnit(String u)                      { this.unit = u; }

    public int        getUnitsPerPack()                      { return unitsPerPack; }
    public void       setUnitsPerPack(int upp)               { this.unitsPerPack = upp; }

    public BigDecimal getBulkCost()                          { return bulkCost; }
    public void       setBulkCost(BigDecimal bc)             { this.bulkCost = bc; }

    public BigDecimal getMarkupRate()                        { return markupRate; }
    public void       setMarkupRate(BigDecimal mr)           { this.markupRate = mr; }

    public int        getQuantityAvailable()                 { return quantityAvailable; }
    public void       setQuantityAvailable(int qa)           { this.quantityAvailable = qa; }

    public int        getMinStockLevel()                     { return minStockLevel; }
    public void       setMinStockLevel(int msl)              { this.minStockLevel = msl; }

    @Override
    public String toString() {
        return "StockItem{id=" + stockItemId + ", saId='" + saItemId
               + "', desc='" + description + "', qty=" + quantityAvailable
               + (isLowStock() ? " [LOW STOCK]" : "") + "}";
    }
}
