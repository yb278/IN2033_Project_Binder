import gui.LoginFrame;
import service.CrossSystemService;
import service.StockApiService;
import service.StockApiServer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Main
 *
 * Application entry point for IPOS-CA.
 * Sets the system look and feel then launches the login screen.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class Main {
    public static void main(String[] args) {

        StockApiServer api = new StockApiServer();
        api.start();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new LoginFrame();
        });
    }
}






