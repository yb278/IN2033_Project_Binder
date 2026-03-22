package dao;

import database.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DiscountPlanDAO
 *
 * Handles loading and saving discount plans.
 * Maps to the {@code discount_plans} table.
 *
 * Supports: CA-34 (Apply Discount Plan)
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class DiscountPlanDAO {

    /**
     * Returns all available discount plans.
     * Each row: [planId, planName, planType, detail description]
     *
     * @return list of plan data rows
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getAllPlans() throws SQLException {
        String sql = "SELECT plan_id, plan_name, plan_type, fixed_rate, flexible_tiers "
                   + "FROM discount_plans ORDER BY plan_id";
        List<Object[]> plans = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("plan_type");
                String detail = "FIXED".equals(type)
                    ? rs.getBigDecimal("fixed_rate") + "% flat rate"
                    : "Flexible tiers (by monthly spend)";
                plans.add(new Object[]{
                    rs.getInt("plan_id"),
                    rs.getString("plan_name"),
                    type,
                    detail
                });
            }
        }
        return plans;
    }

    /**
     * Returns a discount plan by its ID.
     * Row: [planId, planName, planType, fixedRate, flexibleTiers]
     *
     * @param planId the plan ID to look up
     * @return Object array with plan data, or null if not found
     * @throws SQLException if a database error occurs
     */
    public Object[] findById(int planId) throws SQLException {
        String sql = "SELECT * FROM discount_plans WHERE plan_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    rs.getInt("plan_id"),
                    rs.getString("plan_name"),
                    rs.getString("plan_type"),
                    rs.getBigDecimal("fixed_rate"),
                    rs.getString("flexible_tiers")
                };
            }
        }
        return null;
    }

    /**
     * Returns all plan names as a String array — useful for JComboBox population.
     *
     * @return array of plan names in ID order
     * @throws SQLException if a database error occurs
     */
    public String[] getPlanNamesArray() throws SQLException {
        List<String> names = new ArrayList<>();
        for (Object[] row : getAllPlans()) {
            names.add(row[0] + " — " + row[1]);
        }
        return names.toArray(new String[0]);
    }

    /**
     * Calculates the discount amount for a given order value and plan.
     *
     * @param planId       the discount plan ID
     * @param orderValue   the value of the current order
     * @param monthlyTotal the account holder's total orders this month
     * @return the discount amount to apply
     * @throws SQLException if a database error occurs
     */
    public BigDecimal calculateDiscount(int planId, BigDecimal orderValue,
                                        BigDecimal monthlyTotal) throws SQLException {
        Object[] plan = findById(planId);
        if (plan == null) return BigDecimal.ZERO;

        String type = (String) plan[2];
        if ("FIXED".equals(type)) {
            BigDecimal rate = (BigDecimal) plan[3];
            if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return orderValue.multiply(rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                             .setScale(2, RoundingMode.HALF_UP);
        }

        // FLEXIBLE: parse tiers JSON
        String tiersJson = (String) plan[4];
        if (tiersJson == null) return BigDecimal.ZERO;

        double total = monthlyTotal.doubleValue();
        double rate  = 0;
        String[] tiers = tiersJson.replace("[","").replace("]","").split("},\\s*\\{");
        for (String tier : tiers) {
            tier = tier.replace("{","").replace("}","");
            double min = extractJsonDouble(tier, "min");
            double max = extractJsonDouble(tier, "max");
            double r   = extractJsonDouble(tier, "rate");
            if (total >= min && total < max) { rate = r; break; }
        }
        return orderValue.multiply(new BigDecimal(rate / 100.0))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    private double extractJsonDouble(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx < 0) return 0;
            String sub = json.substring(idx + key.length() + 3).trim();
            int end = sub.indexOf(',');
            if (end < 0) end = sub.indexOf('}');
            if (end < 0) end = sub.length();
            return Double.parseDouble(sub.substring(0, end).trim());
        } catch (Exception e) { return 0; }
    }
}
