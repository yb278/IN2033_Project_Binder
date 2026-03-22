package dao;

import database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;

/**
 * MerchantSettingsDAO
 *
 * Handles reading and updating the single merchant_settings row.
 * This row stores pharmacy identity details and the global VAT rate.
 *
 * Supports: CA-21 (Configure VAT), CA-37 (Update Merchant Identity)
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class MerchantSettingsDAO {

    /**
     * Loads all merchant settings as a String array.
     *
     * Index: [0]=pharmacyName  [1]=addressLine1  [2]=addressLine2
     *        [3]=city          [4]=postcode      [5]=phone
     *        [6]=fax           [7]=email         [8]=logoPath
     *        [9]=vatRate
     *
     * @return String array of settings, or null if the row doesn't exist
     * @throws SQLException if a database error occurs
     */
    public String[] getSettings() throws SQLException {
        String sql = "SELECT pharmacy_name, address_line1, address_line2, city, postcode, "
                   + "phone, fax, email, logo_path, vat_rate "
                   + "FROM merchant_settings WHERE id = 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("pharmacy_name"),
                    rs.getString("address_line1"),
                    rs.getString("address_line2"),
                    rs.getString("city"),
                    rs.getString("postcode"),
                    rs.getString("phone"),
                    rs.getString("fax"),
                    rs.getString("email"),
                    rs.getString("logo_path"),
                    rs.getBigDecimal("vat_rate").toPlainString()
                };
            }
        }
        return null;
    }

    /**
     * Returns just the current VAT rate.
     * Called by stock price calculations and the POS panel.
     *
     * @return VAT rate as BigDecimal e.g. 20.00
     * @throws SQLException if a database error occurs
     */
    public BigDecimal getVatRate() throws SQLException {
        String sql = "SELECT vat_rate FROM merchant_settings WHERE id = 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("vat_rate");
        }
        return new BigDecimal("20.00"); // safe default
    }

    /**
     * Updates the global VAT rate. CA-21.
     *
     * @param vatRate new VAT rate e.g. 20.00
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateVatRate(BigDecimal vatRate) throws SQLException {
        String sql = "UPDATE merchant_settings SET vat_rate = ? WHERE id = 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setBigDecimal(1, vatRate);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the pharmacy identity details. CA-37.
     *
     * @param pharmacyName  pharmacy display name
     * @param addressLine1  first line of address
     * @param addressLine2  second line (nullable)
     * @param city          city
     * @param postcode      postcode
     * @param phone         phone number
     * @param fax           fax number (nullable)
     * @param email         contact email
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updateIdentity(String pharmacyName, String addressLine1,
                                   String addressLine2, String city, String postcode,
                                   String phone, String fax, String email) throws SQLException {
        String sql = "UPDATE merchant_settings SET "
                   + "pharmacy_name=?, address_line1=?, address_line2=?, city=?, "
                   + "postcode=?, phone=?, fax=?, email=? WHERE id = 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, pharmacyName);
            ps.setString(2, addressLine1);
            ps.setString(3, addressLine2);
            ps.setString(4, city);
            ps.setString(5, postcode);
            ps.setString(6, phone);
            ps.setString(7, fax);
            ps.setString(8, email);
            return ps.executeUpdate() > 0;
        }
    }
}
