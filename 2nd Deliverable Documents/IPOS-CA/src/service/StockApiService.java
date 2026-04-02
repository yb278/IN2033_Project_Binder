package service;

import dao.StockDAO;
import models.StockItem;

import java.util.List;

public class StockApiService {

    public static String getStockAsJson() {
        try {
            StockDAO dao = new StockDAO();
            List<StockItem> stockList = dao.getAllStockItems();

            StringBuilder json = new StringBuilder();
            json.append("[");

            for (int i = 0; i < stockList.size(); i++) {
                StockItem item = stockList.get(i);

                json.append("{");
                json.append("\"stockItemId\":").append(item.getStockItemId()).append(",");
                json.append("\"saItemId\":\"").append(item.getSaItemId()).append("\",");
                json.append("\"description\":\"").append(item.getDescription()).append("\",");
                json.append("\"quantityAvailable\":").append(item.getQuantityAvailable()).append(",");
                json.append("\"minStockLevel\":").append(item.getMinStockLevel());
                json.append("}");

                if (i < stockList.size() - 1) {
                    json.append(",");
                }
            }

            json.append("]");
            return json.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"Failed to fetch stock\"}";
        }
    }
}