package gui;

import dao.MerchantSettingsDAO;
import dao.StockDAO;
import models.StockItem;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * StockPanel
 * Covers: CA-18 Maintain Stock, CA-19 View Stock,
 *         CA-20 Check Low Stock, CA-21 Configure VAT, CA-22 Update Markup
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class StockPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);
    private static final Color COL_TEXT   = new Color(0x1C2B20);
    private static final Color COL_SUB    = new Color(0x6B7C72);
    private static final Color COL_LOW    = new Color(0xFEF3C7);
    private static final Color COL_LOW_FG = new Color(0xD97706);

    private final StockDAO            stockDAO            = new StockDAO();
    private final MerchantSettingsDAO merchantSettingsDAO = new MerchantSettingsDAO();

    private JTextField        searchField;
    private JCheckBox         lowStockOnly;
    private JTable            stockTable;
    private DefaultTableModel tableModel;
    private JLabel            vatLabel;

    // Stores stock_item_id of selected row
    private int selectedStockItemId = -1;

    public StockPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(COL_BG);
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        loadVatRate();
        loadAllStock();
    }

    // ---------------------------------------------------------------
    // TOP BAR
    // ---------------------------------------------------------------
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(COL_BG);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(COL_BG);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        searchField.setForeground(COL_SUB);
        searchField.setText("Search items...");
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().startsWith("Search")) {
                    searchField.setText(""); searchField.setForeground(COL_TEXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(COL_SUB); searchField.setText("Search items...");
                }
            }
        });
        searchField.addActionListener(e -> onSearch());

        lowStockOnly = new JCheckBox("Low stock only");
        lowStockOnly.setBackground(COL_BG);
        lowStockOnly.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lowStockOnly.addActionListener(e -> onSearch());

        left.add(searchField);
        left.add(lowStockOnly);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(COL_BG);

        JButton vatBtn = makeButton("⚙ Configure VAT", new Color(0xE8F5EE), COL_PRI);
        vatBtn.setBorder(new CompoundBorder(new LineBorder(COL_BORDER,1,true), new EmptyBorder(7,14,7,14)));
        vatBtn.addActionListener(e -> openVatDialog());

        JButton addBtn = makeButton("+ Add Stock Item", COL_PRI, Color.WHITE);
        addBtn.addActionListener(e -> openAddItemDialog());

        right.add(vatBtn);
        right.add(addBtn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // TABLE
    // ---------------------------------------------------------------
    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);

        String[] cols = {"ID","SA Item ID","Description","Qty Available",
                         "Min Level","Bulk Cost (£)","Markup %","Retail Price (£)","Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        stockTable = new JTable(tableModel);
        stockTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        stockTable.setRowHeight(34);
        stockTable.setShowGrid(false);
        stockTable.setIntercellSpacing(new Dimension(0,0));
        stockTable.setBackground(COL_WHITE);
        stockTable.setSelectionBackground(new Color(0xE8F5EE));
        stockTable.setFillsViewportHeight(true);
        stockTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        stockTable.getTableHeader().setBackground(new Color(0xF1F5F9));

        // Highlight low stock rows
        stockTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String status = (String) tableModel.getValueAt(row, 8);
                if (!sel) {
                    if ("LOW STOCK".equals(status)) {
                        c.setBackground(COL_LOW);
                        c.setForeground(col == 8 ? COL_LOW_FG : COL_TEXT);
                    } else {
                        c.setBackground(COL_WHITE);
                        c.setForeground(col == 8 ? new Color(0x22C55E) : COL_TEXT);
                    }
                    if (col == 8) ((JLabel)c).setFont(new Font("SansSerif", Font.BOLD, 12));
                }
                return c;
            }
        });

        int[] widths = {45,110,220,110,90,110,90,130,100};
        for (int i = 0; i < widths.length; i++)
            stockTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Track selected row
        stockTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && stockTable.getSelectedRow() >= 0)
                selectedStockItemId = (int) tableModel.getValueAt(stockTable.getSelectedRow(), 0);
        });

        // Double-click to edit markup
        stockTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditMarkupDialog();
            }
        });

        JScrollPane scroll = new JScrollPane(stockTable);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------
    // BOTTOM STATUS BAR
    // ---------------------------------------------------------------
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xF1F5F9));
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, COL_BORDER),
            new EmptyBorder(8, 12, 8, 12)
        ));
        vatLabel = new JLabel("Current VAT Rate: loading...");
        vatLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        vatLabel.setForeground(COL_SUB);
        bar.add(vatLabel, BorderLayout.WEST);

        JLabel hint = new JLabel("Double-click a row to edit markup rate");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(0xAAAAAA));
        bar.add(hint, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // Data loading — wired to DAOs
    // ---------------------------------------------------------------

    private void loadAllStock() {
        SwingWorker<List<StockItem>, Void> w = new SwingWorker<List<StockItem>, Void>() {
            @Override protected List<StockItem> doInBackground() throws Exception {
                return lowStockOnly.isSelected()
                    ? stockDAO.getLowStockItems()
                    : stockDAO.getAllStockItems();
            }
            @Override protected void done() {
                try { populateTable(get()); }
                catch (Exception ex) { showDbError(ex); }
            }
        };
        w.execute();
    }

    private void onSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty() || term.startsWith("Search") || lowStockOnly.isSelected()) {
            loadAllStock();
            return;
        }
        SwingWorker<List<StockItem>, Void> w = new SwingWorker<List<StockItem>, Void>() {
            @Override protected List<StockItem> doInBackground() throws Exception {
                return stockDAO.searchByDescription(term);
            }
            @Override protected void done() {
                try { populateTable(get()); }
                catch (Exception ex) { showDbError(ex); }
            }
        };
        w.execute();
    }

    private void populateTable(List<StockItem> items) {
        tableModel.setRowCount(0);
        for (StockItem item : items) {
            tableModel.addRow(new Object[]{
                item.getStockItemId(),
                item.getSaItemId(),
                item.getDescription(),
                item.getQuantityAvailable(),
                item.getMinStockLevel(),
                String.format("%.2f", item.getBulkCost()),
                String.format("%.2f", item.getMarkupRate()),
                String.format("%.2f", item.getRetailPriceExVAT()),
                item.isLowStock() ? "LOW STOCK" : "OK"
            });
        }
    }

    private void loadVatRate() {
        SwingWorker<BigDecimal, Void> w = new SwingWorker<BigDecimal, Void>() {
            @Override protected BigDecimal doInBackground() throws Exception {
                return merchantSettingsDAO.getVatRate();
            }
            @Override protected void done() {
                try { vatLabel.setText("Current VAT Rate: " + get().toPlainString() + "%"); }
                catch (Exception ex) { vatLabel.setText("Current VAT Rate: 20.00%"); }
            }
        };
        w.execute();
    }

    // ---------------------------------------------------------------
    // Actions — wired to DAOs
    // ---------------------------------------------------------------

    /** CA-18: Add a new stock item */
    private void openAddItemDialog() {
        StockItemDialog dialog = new StockItemDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (!dialog.wasConfirmed()) return;
        try {
            StockItem item = new StockItem(
                dialog.getSaItemId(),
                dialog.getDescription(),
                dialog.getPackageType(),
                dialog.getUnit(),
                dialog.getUnitsPerPack(),
                dialog.getBulkCost(),
                dialog.getMarkupRate(),
                dialog.getMinStockLevel()
            );
            stockDAO.addStockItem(item);
            JOptionPane.showMessageDialog(this, "Stock item added.", "Added",
                JOptionPane.INFORMATION_MESSAGE);
            loadAllStock();
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-22: Update retail markup rate */
    private void openEditMarkupDialog() {
        if (selectedStockItemId < 0) return;
        int row = stockTable.getSelectedRow();
        String desc    = (String) tableModel.getValueAt(row, 2);
        String current = (String) tableModel.getValueAt(row, 6);
        String input = JOptionPane.showInputDialog(this,
            "Set markup rate for:\n" + desc + "\nCurrent: " + current + "%\nNew %:",
            "Update Markup Rate", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal rate = new BigDecimal(input.trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            stockDAO.updateMarkupRate(selectedStockItemId, rate);
            loadAllStock();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid percentage.", "Invalid",
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-21: Configure VAT rate */
    private void openVatDialog() {
        String current = vatLabel.getText().replace("Current VAT Rate: ", "").replace("%", "");
        String input = JOptionPane.showInputDialog(this,
            "Set global VAT rate (%):\nCurrent: " + current + "%",
            "Configure VAT", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal rate = new BigDecimal(input.trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0)
                throw new NumberFormatException();
            merchantSettingsDAO.updateVatRate(rate);
            vatLabel.setText("Current VAT Rate: " + rate.toPlainString() + "%");
            JOptionPane.showMessageDialog(this, "VAT rate updated to " + rate + "%",
                "Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid rate (0-100).", "Invalid",
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) { showDbError(ex); }
    }

    private void showDbError(Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JButton makeButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    // ---------------------------------------------------------------
    // Add Stock Item dialog
    // ---------------------------------------------------------------
    static class StockItemDialog extends JDialog {
        private boolean confirmed = false;
        private JTextField saIdField, descField, pkgTypeField, unitField,
                           unitsPerPackField, bulkCostField, markupField, minStockField;

        StockItemDialog(JFrame parent) {
            super(parent, "Add Stock Item", true);
            setSize(420, 480);
            setLocationRelativeTo(parent);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));
            panel.setBackground(Color.WHITE);

            saIdField       = addField(panel, "IPOS-SA Item ID (e.g. 100 00001)", "");
            descField       = addField(panel, "Description *", "");
            pkgTypeField    = addField(panel, "Package Type (e.g. box, bottle)", "box");
            unitField       = addField(panel, "Unit (e.g. Caps, ml)", "Caps");
            unitsPerPackField = addField(panel, "Units per Pack *", "20");
            bulkCostField   = addField(panel, "Bulk Cost £ (from InfoPharma) *", "0.00");
            markupField     = addField(panel, "Markup Rate % *", "40.00");
            minStockField   = addField(panel, "Minimum Stock Level *", "100");

            panel.add(Box.createVerticalStrut(12));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnRow.setBackground(Color.WHITE);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            JButton save = new JButton("Add Item");
            save.setBackground(new Color(0x1A6B3C)); save.setForeground(Color.WHITE);
            save.setBorderPainted(false);
            save.addActionListener(e -> {
                if (descField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Description is required.");
                    return;
                }
                confirmed = true;
                dispose();
            });
            btnRow.add(cancel); btnRow.add(save);
            panel.add(btnRow);
            setContentPane(new JScrollPane(panel));
        }

        private JTextField addField(JPanel panel, String label, String value) {
            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lbl); panel.add(Box.createVerticalStrut(4));
            JTextField f = new JTextField(value);
            f.setFont(new Font("SansSerif", Font.PLAIN, 13));
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD6E4DC),1,true), new EmptyBorder(5,8,5,8)));
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(f); panel.add(Box.createVerticalStrut(8));
            return f;
        }

        boolean    wasConfirmed()    { return confirmed; }
        String     getSaItemId()     { return saIdField.getText().trim(); }
        String     getDescription()  { return descField.getText().trim(); }
        String     getPackageType()  { return pkgTypeField.getText().trim(); }
        String     getUnit()         { return unitField.getText().trim(); }
        int        getUnitsPerPack() {
            try { return Integer.parseInt(unitsPerPackField.getText().trim()); } catch (Exception e) { return 1; }
        }
        BigDecimal getBulkCost()     {
            try { return new BigDecimal(bulkCostField.getText().trim()); } catch (Exception e) { return BigDecimal.ZERO; }
        }
        BigDecimal getMarkupRate()   {
            try { return new BigDecimal(markupField.getText().trim()); } catch (Exception e) { return BigDecimal.ZERO; }
        }
        int        getMinStockLevel(){
            try { return Integer.parseInt(minStockField.getText().trim()); } catch (Exception e) { return 0; }
        }
    }
}
