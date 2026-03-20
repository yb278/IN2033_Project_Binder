import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



public class InventoryService {


    private Map<String, StockLevel> stockDatabase = new ConcurrentHashMap<>();

    public InventoryService() {

        stockDatabase.put("10000001", new StockLevel("10000001", "Paracetamol", 10345, 300));
        stockDatabase.put("10000002", new StockLevel("10000002", "Aspirin", 12453, 500));
        stockDatabase.put("10000003", new StockLevel("10000003", "Analgin", 4235, 200));
        stockDatabase.put("20000004", new StockLevel("20000004", "Iodine tincture", 22134, 200));
        stockDatabase.put("20000005", new StockLevel("20000005", "Rhynol", 1908, 300));
    }

    // Deduct stock
    public Map<String, Object> deductStock(String productId, int quantity, String orderRef) {
        Map<String, Object> response = new HashMap<>();

        StockLevel stock = stockDatabase.get(productId);
        if (stock == null) {
            response.put("success", false);
            response.put("error", "Product not found: " + productId);
            return response;
        }

        int currentQty = stock.getQuantity();
        if (currentQty < quantity) {
            response.put("success", false);
            response.put("error", "Insufficient stock. Available: " + currentQty + ", Requested: " + quantity);
            return response;
        }

        stock.setQuantity(currentQty - quantity);

        response.put("success", true);
        response.put("productId", productId);
        response.put("newQuantity", stock.getQuantity());
        response.put("message", "Stock deducted successfully");
        response.put("orderRef", orderRef);

        System.out.println("[" + new Date() + "] Deducted " + quantity + " from " + productId + " for order " + orderRef);

        return response;
    }

    // Add stock
    public Map<String, Object> addStock(String productId, int quantity, String supplierRef) {
        Map<String, Object> response = new HashMap<>();

        StockLevel stock = stockDatabase.get(productId);
        if (stock == null) {
            response.put("success", false);
            response.put("error", "Product not found: " + productId);
            return response;
        }

        stock.setQuantity(stock.getQuantity() + quantity);

        response.put("success", true);
        response.put("productId", productId);
        response.put("newQuantity", stock.getQuantity());
        response.put("message", "Stock added successfully");
        response.put("supplierRef", supplierRef);

        System.out.println("[" + new Date() + "] Added " + quantity + " to " + productId + " from supplier " + supplierRef);

        return response;
    }

    // Get stock level for a product
    public Map<String, Object> getStockLevel(String productId) {
        Map<String, Object> response = new HashMap<>();

        StockLevel stock = stockDatabase.get(productId);
        if (stock == null) {
            response.put("success", false);
            response.put("error", "Product not found: " + productId);
            return response;
        }

        response.put("success", true);
        response.put("productId", stock.getProductId());
        response.put("productName", stock.getProductName());
        response.put("quantity", stock.getQuantity());
        response.put("minimumLevel", stock.getMinimumLevel());
        response.put("status", stock.getQuantity() < stock.getMinimumLevel() ? "LOW" : "OK");

        return response;
    }

    // Get all low stock items
    public Map<String, Object> getLowStockItems() {
        List<Map<String, Object>> lowStockList = new ArrayList<>();

        for (StockLevel stock : stockDatabase.values()) {
            if (stock.getQuantity() < stock.getMinimumLevel()) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", stock.getProductId());
                item.put("productName", stock.getProductName());
                item.put("quantity", stock.getQuantity());
                item.put("minimumLevel", stock.getMinimumLevel());
                item.put("needsOrder", true);
                lowStockList.add(item);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("lowStockItems", lowStockList);
        response.put("count", lowStockList.size());

        return response;
    }
}