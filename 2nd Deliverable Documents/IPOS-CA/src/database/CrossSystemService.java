package service;

import dao.AccountHolderDAO;
import dao.SalesDAO;
import dao.StockDAO;
import models.StockItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CrossSystemService
 *
 * Handles all cross-subsystem communication for IPOS-CA.
 *
 * ── Architecture ─────────────────────────────────────────────────
 * All communication with IPOS-SA (Team A) and IPOS-PU (Team C)
 * is done via REST APIs exchanging JSON.
 *
 * Until the APIs are ready, every method prints to the terminal:
 *
 *   [API→SA] GET  <url>               — we are fetching from SA
 *   [API→SA] POST <url>  { payload }  — we are sending to SA
 *   [API→PU] GET  <url>               — we are fetching from PU
 *   [API→PU] POST <url>  { payload }  — we are sending to PU
 *   [API←PU] RECEIVED <description>   — data arriving from PU
 *
 * ── To complete wiring when APIs are ready ───────────────────────
 * Replace each TODO block with a real HTTP call using Java's
 * HttpClient (Java 11+). See the comment blocks in each method.
 *
 * ── Constants to update ──────────────────────────────────────────
 * Update IPOS_SA_BASE_URL and IPOS_PU_BASE_URL once Teams A and C
 * share their API base URLs and endpoint names.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class CrossSystemService {

    // ---------------------------------------------------------------
    // !! UPDATE THESE once agreed with Teams A and C !!
    // ---------------------------------------------------------------

    /** Base URL for Team A's IPOS-SA REST API */
    private static final String IPOS_SA_BASE_URL = "http://localhost:8081/api";

    /** Base URL for Team C's IPOS-PU REST API */
    private static final String IPOS_PU_BASE_URL = "http://localhost:8082/api";

    // Endpoint paths — update once Teams A/C confirm their routes
    private static final String SA_CATALOGUE_ENDPOINT    = "/catalogue";
    private static final String SA_ORDERS_ENDPOINT       = "/orders";
    private static final String PU_ONLINE_SALES_ENDPOINT = "/online-sales";
    private static final String PU_PAYMENTS_ENDPOINT     = "/payments";

    // Our merchant identifier sent in request headers/payloads
    private static final String CA_MERCHANT_ID = "IPOS-CA-TEAM-B";

    // DAOs for writing to our own DB (used when processing PU data)
    private final StockDAO         stockDAO         = new StockDAO();
    private final SalesDAO         salesDAO         = new SalesDAO();
    private final AccountHolderDAO accountHolderDAO = new AccountHolderDAO();


    // =================================================================
    // IPOS-SA (Team A) — InfoPharma supplier
    // =================================================================

    // ---------------------------------------------------------------
    // CA-29: Read IPOS-SA product catalogue
    // ---------------------------------------------------------------

    /**
     * Fetches the InfoPharma product catalogue from IPOS-SA's API.
     *
     * When wired: GET {IPOS_SA_BASE_URL}/catalogue
     * Expected response JSON:
     *   [ { "itemId": "100 00001", "description": "Paracetamol",
     *       "packageType": "box", "unit": "tabs", "unitsPerPack": 100,
     *       "packageCost": 0.10, "availability": true }, ... ]
     *
     * Each row returned: [itemId, description, packageType, unit,
     *                     unitsPerPack, packageCost, availability]
     *
     * @return list of catalogue rows, empty until API is wired
     */
    public List<Object[]> getIposSaCatalogue() {
        String url = IPOS_SA_BASE_URL + SA_CATALOGUE_ENDPOINT;
        System.out.println("[API→SA] GET " + url);
        System.out.println("[API→SA] Headers: { X-Merchant-ID: " + CA_MERCHANT_ID + " }");

        // TODO: uncomment when Team A's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("X-Merchant-ID", CA_MERCHANT_ID)
        //         .GET().build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() == 200) {
        //         return parseCatalogueJson(res.body()); // parse JSON array
        //     }
        // } catch (Exception e) {
        //     System.err.println("[API→SA] Request failed: " + e.getMessage());
        // }

        System.out.println("[API→SA] ⚠ API not yet connected — returning empty catalogue");
        return new ArrayList<>();
    }

    /**
     * Checks whether IPOS-SA's API is reachable.
     * When wired: GET {IPOS_SA_BASE_URL}/health → 200 OK
     */
    public boolean isIposSaReachable() {
        String url = IPOS_SA_BASE_URL + "/health";
        System.out.println("[API→SA] GET " + url + "  (health check)");

        // TODO: HTTP GET — return true if response status is 200
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        //     return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        // } catch (Exception e) { return false; }

        System.out.println("[API→SA] ⚠ API not yet connected — reporting unreachable");
        return false;
    }

    // ---------------------------------------------------------------
    // CA-29: Submit an order to IPOS-SA
    // ---------------------------------------------------------------

    /**
     * Submits a new purchase order to IPOS-SA via their API.
     *
     * When wired: POST {IPOS_SA_BASE_URL}/orders
     * Payload JSON:
     *   { "merchantId": "IPOS-CA-TEAM-B",
     *     "items": [ { "itemId": "100 00001", "quantity": 500 }, ... ] }
     * Expected response JSON:
     *   { "orderRef": "IP2034", "status": "PENDING" }
     *
     * @param merchantAccountNo  our pharmacy's account number with InfoPharma
     * @param items              list of [saItemId, quantity] pairs
     * @return the order reference assigned by IPOS-SA, or null on failure
     */
    public String submitOrderToIposSa(String merchantAccountNo, List<Object[]> items) {
        String url = IPOS_SA_BASE_URL + SA_ORDERS_ENDPOINT;

        // Build JSON payload
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"merchantId\": \"").append(merchantAccountNo).append("\", \"items\": [");
        for (int i = 0; i < items.size(); i++) {
            Object[] item = items.get(i);
            sb.append("{ \"itemId\": \"").append(item[0])
              .append("\", \"quantity\": ").append(item[1]).append(" }");
            if (i < items.size() - 1) sb.append(", ");
        }
        sb.append("] }");

        System.out.println("[API→SA] POST " + url);
        System.out.println("[API→SA] Payload: " + sb);

        // TODO: uncomment when Team A's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("Content-Type", "application/json")
        //         .header("X-Merchant-ID", CA_MERCHANT_ID)
        //         .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
        //         .build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() == 201) {
        //         // parse { "orderRef": "IP2034" } from res.body()
        //         return extractJsonField(res.body(), "orderRef");
        //     }
        // } catch (Exception e) {
        //     System.err.println("[API→SA] POST failed: " + e.getMessage());
        // }

        System.out.println("[API→SA] ⚠ API not yet connected — order not submitted");
        return null;
    }


    // =================================================================
    // IPOS-PU (Team C) — Online pharmacy portal
    // =================================================================

    // ---------------------------------------------------------------
    // Fetch online sales from IPOS-PU
    // ---------------------------------------------------------------

    /**
     * Fetches all online sales from IPOS-PU's API, including delivery
     * address and current fulfilment status.
     *
     * When wired: GET {IPOS_PU_BASE_URL}/online-sales
     * Expected response JSON:
     *   [ { "saleId": 1, "items": "Paracetamol x2", "total": 0.28,
     *       "deliveryAddress": "12 Oak St, London SE1 1AA",
     *       "status": "RECEIVED", "saleDate": "2026-03-15" }, ... ]
     *
     * Each row returned: [saleId, items, total, deliveryAddress, status, date]
     *
     * @return list of online sale rows, empty until API is wired
     */
    public List<Object[]> getOnlineSalesWithStatus() {
        String url = IPOS_PU_BASE_URL + PU_ONLINE_SALES_ENDPOINT;
        System.out.println("[API→PU] GET " + url);
        System.out.println("[API→PU] Headers: { X-Merchant-ID: " + CA_MERCHANT_ID + " }");

        // TODO: uncomment when Team C's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("X-Merchant-ID", CA_MERCHANT_ID)
        //         .GET().build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() == 200) {
        //         return parseOnlineSalesJson(res.body());
        //     }
        // } catch (Exception e) {
        //     System.err.println("[API→PU] GET failed: " + e.getMessage());
        // }

        System.out.println("[API→PU] ⚠ API not yet connected — returning empty sales list");
        return new ArrayList<>();
    }

    /**
     * Polls IPOS-PU for unprocessed online sales, deducts stock from our DB,
     * records the sale, then notifies PU the sale has been received.
     *
     * When wired:
     *   Step 1 — GET  {IPOS_PU_BASE_URL}/online-sales?processed=false
     *   Step 2 — For each sale: deduct stock + record in ipos_ca.sales
     *   Step 3 — POST {IPOS_PU_BASE_URL}/online-sales/{id}/received
     *             to notify PU we have processed it
     *
     * @return number of sales processed (0 until API is wired)
     */
    public int processOnlineSales() {
        String url = IPOS_PU_BASE_URL + PU_ONLINE_SALES_ENDPOINT + "?processed=false";
        System.out.println("[API→PU] GET " + url + "  (fetching unprocessed online sales)");

        // TODO: uncomment and complete when Team C's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() == 200) {
        //         List<OnlineSale> sales = parseOnlineSalesFromJson(res.body());
        //         int count = 0;
        //         for (OnlineSale sale : sales) {
        //             try {
        //                 // 1. Deduct stock
        //                 StockItem item = stockDAO.findBySaItemId(sale.getItemId());
        //                 if (item != null) stockDAO.decreaseStock(item.getStockItemId(), sale.getQuantity());
        //                 // 2. Record sale in our DB
        //                 Sale s = buildSaleFromOnlineSale(sale);
        //                 salesDAO.recordSale(s, stockDAO, accountHolderDAO);
        //                 // 3. Notify PU
        //                 notifyPuSaleReceived(sale.getSaleId(), client);
        //                 count++;
        //             } catch (Exception e) {
        //                 System.err.println("[API←PU] Failed to process sale " + sale.getSaleId() + ": " + e.getMessage());
        //             }
        //         }
        //         return count;
        //     }
        // } catch (Exception e) {
        //     System.err.println("[API→PU] GET failed: " + e.getMessage());
        // }

        System.out.println("[API→PU] ⚠ API not yet connected — no sales processed");
        return 0;
    }

    // ---------------------------------------------------------------
    // Update online sale status
    // ---------------------------------------------------------------

    /**
     * Updates the fulfilment status of an online sale, notifying IPOS-PU.
     * Status flow: RECEIVED → PICKING → DISPATCHED → DELIVERED
     *
     * When wired: POST {IPOS_PU_BASE_URL}/online-sales/{saleId}/status
     * Payload JSON: { "status": "DISPATCHED", "updatedBy": "IPOS-CA-TEAM-B" }
     *
     * @param puSaleId  the sale ID in IPOS-PU's system
     * @param newStatus the new status string
     */
    public void updateOnlineSaleStatus(int puSaleId, String newStatus) throws SQLException {
        String url = IPOS_PU_BASE_URL + PU_ONLINE_SALES_ENDPOINT + "/" + puSaleId + "/status";
        String payload = "{ \"status\": \"" + newStatus + "\", "
                       + "\"updatedBy\": \"" + CA_MERCHANT_ID + "\" }";

        System.out.println("[API→PU] POST " + url);
        System.out.println("[API→PU] Payload: " + payload);

        // TODO: uncomment when Team C's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("Content-Type", "application/json")
        //         .POST(HttpRequest.BodyPublishers.ofString(payload))
        //         .build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() != 200)
        //         System.err.println("[API→PU] Status update rejected: " + res.body());
        // } catch (Exception e) {
        //     System.err.println("[API→PU] POST failed: " + e.getMessage());
        // }

        System.out.println("[API→PU] ⚠ API not yet connected — status update not sent to PU");
    }

    // ---------------------------------------------------------------
    // Card payment flow — IPOS-CA sends details to IPOS-PU
    // who forward to PayPal/payment processor for clearance
    // ---------------------------------------------------------------

    /**
     * STEP 1: Sends card payment details to IPOS-PU for processing.
     *
     * IPOS-PU acts as the payment processor integration layer for the whole
     * system (per the brief). They forward card details to PayPal or similar.
     *
     * When wired: POST {IPOS_PU_BASE_URL}/payments/submit
     * Payload JSON: card details from our sales record
     * Response JSON: { "paymentRef": "PU-PAY-001", "status": "PENDING" }
     *
     * @param saleId our internal sale_id for the CARD payment to submit
     * @return the payment reference assigned by IPOS-PU, or null on failure
     */
    public String submitCardPaymentToPu(int saleId) throws SQLException {
        // Read card details from our own DB
        String selectSql = "SELECT invoice_number, total_amount, card_type, "
                         + "card_first_four, card_last_four, card_expiry "
                         + "FROM sales WHERE sale_id = ? AND payment_method = 'CARD'";

        try (PreparedStatement ps = database.DatabaseConnection.getConnection()
                .prepareStatement(selectSql)) {
            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("[API→PU] ⚠ No CARD sale found with ID " + saleId);
                return null;
            }

            String url = IPOS_PU_BASE_URL + PU_PAYMENTS_ENDPOINT + "/submit";
            String payload = "{ "
                + "\"merchantId\": \""    + CA_MERCHANT_ID                      + "\", "
                + "\"saleId\": "          + saleId                               + ", "
                + "\"invoiceNumber\": \"" + rs.getString("invoice_number")       + "\", "
                + "\"amount\": "          + rs.getBigDecimal("total_amount")     + ", "
                + "\"cardType\": \""      + nvl(rs.getString("card_type"))       + "\", "
                + "\"cardFirstFour\": \"" + nvl(rs.getString("card_first_four")) + "\", "
                + "\"cardLastFour\": \""  + nvl(rs.getString("card_last_four"))  + "\", "
                + "\"cardExpiry\": \""    + nvl(rs.getString("card_expiry"))     + "\" }";

            System.out.println("[API→PU] POST " + url);
            System.out.println("[API→PU] Payload: " + payload);
            System.out.println("[API→PU] → PU will forward to PayPal/payment processor");

            // TODO: uncomment when Team C's API is ready
            // HttpClient client = HttpClient.newHttpClient();
            // HttpRequest req = HttpRequest.newBuilder()
            //     .uri(URI.create(url))
            //     .header("Content-Type", "application/json")
            //     .POST(HttpRequest.BodyPublishers.ofString(payload))
            //     .build();
            // HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            // if (res.statusCode() == 200 || res.statusCode() == 201) {
            //     return extractJsonField(res.body(), "paymentRef");
            // }

            System.out.println("[API→PU] ⚠ API not yet connected — details printed above for demo");
            return "PU-STUB-" + saleId; // stub ref so the UI can proceed
        }
    }

    /**
     * STEP 2: Checks the clearance status of a card payment with IPOS-PU.
     *
     * IPOS-PU queries PayPal and returns the result. We must receive
     * CLEARED confirmation before marking the payment as reconciled.
     *
     * When wired: GET {IPOS_PU_BASE_URL}/payments/{paymentRef}/status
     * Response JSON: { "paymentRef": "PU-PAY-001",
     *                  "status": "CLEARED" | "PENDING" | "REJECTED",
     *                  "processedAt": "2026-03-29T14:22:00" }
     *
     * @param paymentRef the reference returned by submitCardPaymentToPu()
     * @return "CLEARED", "PENDING", "REJECTED", or "UNKNOWN" if API not connected
     */
    public String checkPaymentClearanceStatus(String paymentRef) {
        String url = IPOS_PU_BASE_URL + PU_PAYMENTS_ENDPOINT + "/" + paymentRef + "/status";
        System.out.println("[API→PU] GET " + url + "  (checking payment clearance status)");

        // TODO: uncomment when Team C's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        //     HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        //     if (res.statusCode() == 200) {
        //         return extractJsonField(res.body(), "status"); // "CLEARED", "PENDING", "REJECTED"
        //     }
        // } catch (Exception e) {
        //     System.err.println("[API→PU] GET failed: " + e.getMessage());
        // }

        System.out.println("[API→PU] ⚠ API not yet connected — returning CLEARED for demo");
        return "CLEARED"; // stub: assume cleared so demo can proceed
    }

    /**
     * STEP 3: Marks a card payment as reconciled in our own DB.
     *
     * Only called after checkPaymentClearanceStatus() returns "CLEARED".
     * This is our internal confirmation that PU/PayPal processed the payment.
     *
     * @param saleId our internal sale_id
     */
    public void markPaymentReconciled(int saleId) throws SQLException {
        try {
            String sql = "UPDATE sales SET pu_reconciled = TRUE WHERE sale_id = ?";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection()
                    .prepareStatement(sql)) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
                System.out.println("[DB] Sale #" + saleId + " marked pu_reconciled = TRUE");
            }
        } catch (SQLException ex) {
            System.err.println("[DB] ⚠ pu_reconciled column missing — run in MySQL:");
            System.err.println("     ALTER TABLE sales ADD COLUMN pu_reconciled BOOLEAN DEFAULT FALSE;");
        }
    }

    /**
     * Convenience method: runs all three steps in sequence.
     *
     * Full card payment reconciliation flow:
     *   Step 1 — POST card details to IPOS-PU (they forward to PayPal)
     *   Step 2 — GET clearance status from IPOS-PU
     *   Step 3 — If CLEARED: mark reconciled in our DB
     *
     * @param saleId our internal sale_id for the CARD payment
     * @return "CLEARED", "PENDING", "REJECTED", or error message
     */
    public String reconcileCardPayment(int saleId) throws SQLException {
        System.out.println("[PAYMENT] Starting reconciliation for Sale #" + saleId);

        // Step 1 — send to PU
        String paymentRef = submitCardPaymentToPu(saleId);
        if (paymentRef == null) {
            return "ERROR: Could not submit card details to IPOS-PU";
        }
        System.out.println("[PAYMENT] Submitted to PU, ref: " + paymentRef);

        // Step 2 — check clearance status
        String status = checkPaymentClearanceStatus(paymentRef);
        System.out.println("[PAYMENT] Clearance status from PU: " + status);

        // Step 3 — mark reconciled only if cleared
        if ("CLEARED".equals(status)) {
            markPaymentReconciled(saleId);
            System.out.println("[PAYMENT] ✓ Sale #" + saleId + " reconciled successfully");
        } else if ("REJECTED".equals(status)) {
            System.out.println("[PAYMENT] ✗ Sale #" + saleId + " payment REJECTED by processor");
        } else {
            System.out.println("[PAYMENT] ⏳ Sale #" + saleId + " payment still PENDING");
        }

        return status;
    }

    // Keep old method name as alias so OnlineSalesPanel still compiles
    public void markCardPaymentReconciled(int saleId) throws SQLException {
        reconcileCardPayment(saleId);
    }

    // ---------------------------------------------------------------
    // CA-18: Our stock catalogue exposed to IPOS-PU
    // ---------------------------------------------------------------

    /**
     * Logs what our stock catalogue looks like as JSON —
     * this is what IPOS-PU would GET from our API to show the
     * merchant's product catalogue online.
     *
     * In a full implementation CA would run its own REST server
     * (e.g. a simple HttpServer or Spring Boot) exposing this endpoint.
     * IPOS-PU calls it to populate their online store.
     */
    public void logStockExposureToIposPu() throws SQLException {
        List<StockItem> items = stockDAO.getAllStockItems();
        System.out.println("[API←PU] IPOS-PU would GET our stock catalogue");
        System.out.println("[API←PU] We would return " + items.size() + " items as JSON:");
        for (StockItem item : items) {
            System.out.println("[API←PU]   { \"id\": " + item.getStockItemId()
                + ", \"saItemId\": \"" + item.getSaItemId()
                + "\", \"description\": \"" + item.getDescription()
                + "\", \"qty\": " + item.getQuantityAvailable()
                + ", \"retailPrice\": " + item.getRetailPriceExVAT() + " }");
        }
    }

    /**
     * Checks whether IPOS-PU's API is reachable.
     * When wired: GET {IPOS_PU_BASE_URL}/health → 200 OK
     */
    public boolean isIposPuReachable() {
        String url = IPOS_PU_BASE_URL + "/health";
        System.out.println("[API→PU] GET " + url + "  (health check)");

        // TODO: uncomment when Team C's API is ready
        // try {
        //     HttpClient client = HttpClient.newHttpClient();
        //     HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        //     return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        // } catch (Exception e) { return false; }

        System.out.println("[API→PU] ⚠ API not yet connected — reporting unreachable");
        return false;
    }

    // ---------------------------------------------------------------
    // Private helper
    // ---------------------------------------------------------------

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
