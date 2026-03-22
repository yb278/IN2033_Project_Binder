package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection
 *
 * Provides a singleton JDBC connection to the IPOS-CA MySQL database.
 * Used by all DAO classes to obtain a shared connection.
 *
 * Configuration: update DB_HOST, DB_NAME, DB_USER, DB_PASSWORD
 * to match your MySQL installation before running.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class DatabaseConnection {

    // ---------------------------------------------------------------
    // Connection parameters – update these to match your MySQL setup
    // ---------------------------------------------------------------
    private static final String DB_HOST     = "localhost";
    private static final String DB_PORT     = "3306";
    private static final String DB_NAME     = "ipos_ca";
    private static final String DB_USER     = "root";         // change as needed
    private static final String DB_PASSWORD = "!Assword2005"; // change as needed

    private static final String JDBC_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    /** Singleton connection instance */
    private static Connection connection = null;

    /**
     * Private constructor – prevents instantiation.
     * Use {@link #getConnection()} to obtain the connection.
     */
    private DatabaseConnection() {}

    /**
     * Returns the singleton JDBC connection, creating it if necessary.
     * Reconnects automatically if the connection has been closed.
     *
     * @return a live {@link Connection} to the ipos_ca database
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Reconnect if connection is null or has been closed
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver"); // load MySQL JDBC driver
                connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                System.out.println("[DB] Connected to ipos_ca database successfully.");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC driver not found. Ensure mysql-connector-j is on the classpath.", e);
        }
        return connection;
    }

    /**
     * Closes the singleton connection and releases database resources.
     * Call this when the application shuts down.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DB] Database connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }
}
