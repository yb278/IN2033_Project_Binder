package dao;

import database.DatabaseConnection;

import java.sql.*;

/**
 * TemplateDAO
 *
 * Handles loading and saving reminder and receipt templates.
 * Maps to the {@code templates} table.
 *
 * Supports: CA-35 (Update Reminder Template), CA-36 (Update Receipt Template)
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class TemplateDAO {

    /** Template type constants matching the database ENUM */
    public static final String FIRST_REMINDER  = "FIRST_REMINDER";
    public static final String SECOND_REMINDER = "SECOND_REMINDER";
    public static final String RECEIPT         = "RECEIPT";

    /**
     * Loads a template body by its type.
     *
     * @param templateType one of FIRST_REMINDER, SECOND_REMINDER, RECEIPT
     * @return the template body string, or null if not found
     * @throws SQLException if a database error occurs
     */
    public String getTemplate(String templateType) throws SQLException {
        String sql = "SELECT template_body FROM templates WHERE template_type = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, templateType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("template_body");
        }
        return null;
    }

    /**
     * Saves an updated template body.
     *
     * @param templateType the type to update
     * @param newBody      the new template text
     * @return true if the update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean saveTemplate(String templateType, String newBody) throws SQLException {
        String sql = "UPDATE templates SET template_body = ? WHERE template_type = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newBody);
            ps.setString(2, templateType);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fills in all placeholders in a template with real values.
     *
     * @param templateType   the template to load
     * @param holderName     account holder name (or "Walk-in")
     * @param invoiceNo      invoice number
     * @param accountNo      account holder ID as string
     * @param amount         formatted amount string e.g. "£125.49"
     * @param invoiceDate    date of the original invoice
     * @param paymentDueDate payment due date
     * @param pharmacistName the logged-in staff member's name
     * @return the fully populated template string ready to display/print
     * @throws SQLException if a database error occurs
     */
    public String fillTemplate(String templateType, String holderName, String invoiceNo,
                                String accountNo, String amount, String invoiceDate,
                                String paymentDueDate, String pharmacistName) throws SQLException {
        String body = getTemplate(templateType);
        if (body == null) return "Template not found: " + templateType;

        return body
            .replace("{HOLDER_NAME}",      holderName     != null ? holderName     : "")
            .replace("{INVOICE_NO}",       invoiceNo      != null ? invoiceNo      : "")
            .replace("{ACCOUNT_NO}",       accountNo      != null ? accountNo      : "N/A")
            .replace("{AMOUNT}",           amount         != null ? amount         : "")
            .replace("{INVOICE_DATE}",     invoiceDate    != null ? invoiceDate    : "")
            .replace("{PAYMENT_DUE_DATE}", paymentDueDate != null ? paymentDueDate : "")
            .replace("{PHARMACIST_NAME}",  pharmacistName != null ? pharmacistName : "")
            .replace("{DATE}",             new java.util.Date().toString());
    }
}
