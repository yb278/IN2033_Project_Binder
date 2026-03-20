import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import java.util.Map;
import java.util.Date;

public class Server {

    private static final String VALID_TOKEN = "Bearer secret123";
    private static InventoryService inventoryService = new InventoryService();

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(7000);

        System.out.println("IPOS-CA Inventory API running on http://localhost:7000");
        System.out.println("Health check: http://localhost:7000/health");
        System.out.println("API endpoints:");
        System.out.println("  POST /stock/deduct       (deduct stock for online sale)");
        System.out.println("  POST /stock/add          (add stock from supplier)");
        System.out.println("  GET  /stock/low-stock    (get low stock items)");
        System.out.println("  GET  /stock/{id}         (check stock level)");
        System.out.println("Auth: Bearer secret123");

        // Public endpoints
        app.get("/", ctx -> ctx.result("IPOS-CA Inventory API is running - Use /health for status"));
        app.get("/health", ctx -> {
            ctx.json(Map.of(
                    "status", "UP",
                    "service", "IPOS-CA Inventory API",
                    "timestamp", System.currentTimeMillis()
            ));
        });

        //  Auth
        app.before("/stock/*", ctx -> {
            String token = ctx.header("Authorization");
            if (!VALID_TOKEN.equals(token)) {
                throw new UnauthorizedResponse("Unauthorized - Invalid or missing token");
            }
        });

        //  Deduct stock
        app.post("/stock/deduct", ctx -> {
            Map<String, Object> request = ctx.bodyAsClass(Map.class);
            String productId = (String) request.get("productId");
            Number quantityNum = (Number) request.get("quantity");
            int quantity = quantityNum != null ? quantityNum.intValue() : 0;
            String orderRef = (String) request.get("orderRef");

            if (productId == null || productId.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "productId is required"));
                return;
            }
            if (quantity <= 0) {
                ctx.status(400).json(Map.of("error", "quantity must be positive"));
                return;
            }
            if (orderRef == null || orderRef.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "orderRef is required"));
                return;
            }

            Map<String, Object> result = inventoryService.deductStock(productId, quantity, orderRef);
            Boolean success = (Boolean) result.get("success");
            ctx.status(success != null && success ? 200 : 400).json(result);
        });

        // Add stock
        app.post("/stock/add", ctx -> {
            Map<String, Object> request = ctx.bodyAsClass(Map.class);
            String productId = (String) request.get("productId");
            Number quantityNum = (Number) request.get("quantity");
            int quantity = quantityNum != null ? quantityNum.intValue() : 0;
            String supplierRef = (String) request.get("supplierRef");

            if (productId == null || productId.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "productId is required"));
                return;
            }
            if (quantity <= 0) {
                ctx.status(400).json(Map.of("error", "quantity must be positive"));
                return;
            }

            Map<String, Object> result = inventoryService.addStock(
                    productId, quantity, supplierRef != null ? supplierRef : "unknown"
            );
            Boolean success = (Boolean) result.get("success");
            ctx.status(success != null && success ? 200 : 400).json(result);
        });

        //  Low stock endpoint
        app.get("/stock/low-stock", ctx -> {
            ctx.status(200).json(inventoryService.getLowStockItems());
        });

        //  Get stock level endpoint
        app.get("/stock/{productId}", ctx -> {
            String productId = ctx.pathParam("productId");
            Map<String, Object> result = inventoryService.getStockLevel(productId);
            Boolean success = (Boolean) result.get("success");
            ctx.status(success != null && success ? 200 : 404).json(result);
        });

        // Exception handler
        app.exception(Exception.class, (e, ctx) -> {
            System.err.println("Error: " + e.getMessage());
            if (ctx.statusCode() != 401) {
                ctx.status(500).json(Map.of("error", "Internal server error: " + e.getMessage()));
            }
        });

        System.out.println("\n Server ready! Test with:");
        System.out.println("curl http://localhost:7000/health");
        System.out.println("curl -X POST http://localhost:7000/stock/deduct -H \"Authorization: Bearer secret123\" -H \"Content-Type: application/json\" -d \"{\\\"productId\\\":\\\"10000001\\\",\\\"quantity\\\":10,\\\"orderRef\\\":\\\"SALE-001\\\"}\"");        System.out.println("curl -X GET http://localhost:7000/stock/low-stock -H \"Authorization: Bearer secret123\"");
    }
}