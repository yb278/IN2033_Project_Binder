package service;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class StockApiServer {

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);

            server.createContext("/api/stock", this::handleStock);

            server.start();

            System.out.println("API running at http://localhost:8085/api/stock");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStock(HttpExchange exchange) {
        try {
            service.CrossSystemService service = new service.CrossSystemService();

            String json = service.getStockCatalogueJsonForPu();

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length());

            OutputStream os = exchange.getResponseBody();
            os.write(json.getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}