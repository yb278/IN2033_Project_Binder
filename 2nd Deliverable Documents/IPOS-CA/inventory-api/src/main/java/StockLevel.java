

public class StockLevel {
    private String productId;
    private String productName;
    private int quantity;
    private int minimumLevel;

    // Constructor
    public StockLevel(String productId, String productName, int quantity, int minimumLevel) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.minimumLevel = minimumLevel;
    }

    // Getters and setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinimumLevel() { return minimumLevel; }
    public void setMinimumLevel(int minimumLevel) { this.minimumLevel = minimumLevel; }
}