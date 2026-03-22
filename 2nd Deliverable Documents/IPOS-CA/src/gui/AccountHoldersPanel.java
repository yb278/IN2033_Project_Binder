package gui;

import dao.AccountHolderDAO;
import dao.DiscountPlanDAO;
import models.AccountHolder;
import models.AccountHolder.AccountStatus;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * AccountHoldersPanel
 *
 * Covers: CA-03 Create, CA-05 Update, CA-06 View Status,
 *         CA-09 Update Status, CA-10 Record Payment,
 *         CA-32 Query Balance, CA-33 Set Credit Limit, CA-34 Apply Discount
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class AccountHoldersPanel extends JPanel {

    private static final Color COL_BG      = new Color(0xF5F7FA);
    private static final Color COL_WHITE   = Color.WHITE;
    private static final Color COL_PRIMARY = new Color(0x1A6B3C);
    private static final Color COL_BORDER  = new Color(0xD6E4DC);
    private static final Color COL_TEXT    = new Color(0x1C2B20);
    private static final Color COL_SUB     = new Color(0x6B7C72);
    private static final Color COL_NORMAL  = new Color(0x22C55E);
    private static final Color COL_SUSP    = new Color(0xF59E0B);
    private static final Color COL_DEFAULT = new Color(0xEF4444);

    private final AccountHolderDAO accountHolderDAO = new AccountHolderDAO();
    private final DiscountPlanDAO  discountPlanDAO  = new DiscountPlanDAO();

    private JTextField         searchField;
    private JTable             holdersTable;
    private DefaultTableModel  tableModel;
    private JLabel             detailName, detailStatus, detailBalance, detailCredit;
    private JButton            editBtn, recordPaymentBtn, setCreditBtn,
                               applyDiscountBtn, updateStatusBtn;

    // Holds the holder_id of whichever row is selected
    private int selectedHolderId = -1;

    public AccountHoldersPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(COL_BG);
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildDetailPanel(), BorderLayout.EAST);
        loadAllHolders(); // initial load
    }

    // ---------------------------------------------------------------
    // TOP BAR
    // ---------------------------------------------------------------
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(COL_BG);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setBackground(COL_BG);

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));
        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.setForeground(COL_SUB);
        searchField.setText("Search by name...");
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().startsWith("Search")) {
                    searchField.setText(""); searchField.setForeground(COL_TEXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(COL_SUB); searchField.setText("Search by name...");
                }
            }
        });
        searchField.addActionListener(e -> onSearch());

        JButton searchBtn = makeButton("Search", COL_PRIMARY, Color.WHITE);
        searchBtn.addActionListener(e -> onSearch());

        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchBtn,   BorderLayout.EAST);

        JButton newBtn = makeButton("+ New Account Holder", new Color(0xE8F5EE), COL_PRIMARY);
        newBtn.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(8, 16, 8, 16)));
        newBtn.addActionListener(e -> openCreateDialog());

        bar.add(searchRow, BorderLayout.CENTER);
        bar.add(newBtn,    BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // TABLE
    // ---------------------------------------------------------------
    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);

        String[] cols = {"ID", "Name", "Email", "Phone", "Status", "Balance (£)", "Credit Limit (£)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        holdersTable = new JTable(tableModel);
        holdersTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        holdersTable.setRowHeight(34);
        holdersTable.setShowGrid(false);
        holdersTable.setIntercellSpacing(new Dimension(0, 0));
        holdersTable.setSelectionBackground(new Color(0xE8F5EE));
        holdersTable.setBackground(COL_WHITE);
        holdersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        holdersTable.getTableHeader().setBackground(new Color(0xF1F5F9));
        holdersTable.setFillsViewportHeight(true);

        // Status column colour renderer
        holdersTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String s = v == null ? "" : v.toString();
                l.setFont(new Font("SansSerif", Font.BOLD, 12));
                if (!sel) switch (s) {
                    case "NORMAL":     l.setForeground(COL_NORMAL);  l.setBackground(COL_WHITE); break;
                    case "SUSPENDED":  l.setForeground(COL_SUSP);    l.setBackground(COL_WHITE); break;
                    case "IN_DEFAULT": l.setForeground(COL_DEFAULT); l.setBackground(COL_WHITE); break;
                    default:           l.setForeground(COL_TEXT);    l.setBackground(COL_WHITE);
                }
                return l;
            }
        });

        int[] widths = {50, 180, 200, 130, 110, 110, 130};
        for (int i = 0; i < widths.length; i++)
            holdersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        holdersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && holdersTable.getSelectedRow() >= 0)
                onRowSelected(holdersTable.getSelectedRow());
        });

        JScrollPane scroll = new JScrollPane(holdersTable);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));
        scroll.getViewport().setBackground(COL_WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------
    // DETAIL PANEL (right side)
    // ---------------------------------------------------------------
    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(0, 1, 0, 0, COL_BORDER),
            new EmptyBorder(20, 20, 20, 20)
        ));
        panel.setPreferredSize(new Dimension(240, 0));

        JLabel heading = new JLabel("Account Details");
        heading.setFont(new Font("Georgia", Font.BOLD, 15));
        heading.setForeground(COL_TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(16));
        panel.add(makeDivider());
        panel.add(Box.createVerticalStrut(14));

        detailName    = makeDetailLabel("Select a row");
        detailStatus  = makeDetailLabel("—");
        detailBalance = makeDetailLabel("—");
        detailCredit  = makeDetailLabel("—");

        panel.add(makeDetailRow("Name:",         detailName));
        panel.add(Box.createVerticalStrut(10));
        panel.add(makeDetailRow("Status:",       detailStatus));
        panel.add(Box.createVerticalStrut(10));
        panel.add(makeDetailRow("Balance:",      detailBalance));
        panel.add(Box.createVerticalStrut(10));
        panel.add(makeDetailRow("Credit Limit:", detailCredit));
        panel.add(Box.createVerticalStrut(20));
        panel.add(makeDivider());
        panel.add(Box.createVerticalStrut(16));

        editBtn          = makeActionButton("✏  Edit Details",        new Color(0x3B82F6));
        recordPaymentBtn = makeActionButton("💰  Record Payment",      new Color(0x22C55E));
        setCreditBtn     = makeActionButton("💳  Set Credit Limit",    new Color(0x8B5CF6));
        applyDiscountBtn = makeActionButton("🏷  Apply Discount Plan", new Color(0xF59E0B));
        updateStatusBtn  = makeActionButton("🔄  Update Status",       new Color(0xEF4444));

        editBtn.addActionListener(e -> openEditDialog());
        recordPaymentBtn.addActionListener(e -> openRecordPaymentDialog());
        setCreditBtn.addActionListener(e -> openSetCreditDialog());
        applyDiscountBtn.addActionListener(e -> openApplyDiscountDialog());
        updateStatusBtn.addActionListener(e -> openUpdateStatusDialog());

        for (JButton btn : new JButton[]{editBtn, recordPaymentBtn,
                setCreditBtn, applyDiscountBtn, updateStatusBtn}) {
            btn.setEnabled(false);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(8));
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ---------------------------------------------------------------
    // Data loading — wired to AccountHolderDAO
    // ---------------------------------------------------------------

    private void loadAllHolders() {
        runInBackground(() -> accountHolderDAO.getAllAccountHolders(), this::populateTable);
    }

    private void onSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty() || term.startsWith("Search")) {
            loadAllHolders();
            return;
        }
        runInBackground(() -> accountHolderDAO.searchByName(term), this::populateTable);
    }

    private void populateTable(List<AccountHolder> holders) {
        tableModel.setRowCount(0);
        selectedHolderId = -1;
        setDetailEnabled(false);
        for (AccountHolder ah : holders) {
            tableModel.addRow(new Object[]{
                ah.getHolderId(),
                ah.getFullName(),
                ah.getEmail(),
                ah.getPhone(),
                ah.getAccountStatus().name(),
                String.format("%.2f", ah.getOutstandingBalance()),
                String.format("%.2f", ah.getCreditLimit())
            });
        }
    }

    private void onRowSelected(int row) {
        selectedHolderId = (int) tableModel.getValueAt(row, 0);
        detailName.setText((String) tableModel.getValueAt(row, 1));
        String status = (String) tableModel.getValueAt(row, 4);
        detailStatus.setText(status);
        switch (status) {
            case "NORMAL":     detailStatus.setForeground(COL_NORMAL);  break;
            case "SUSPENDED":  detailStatus.setForeground(COL_SUSP);    break;
            case "IN_DEFAULT": detailStatus.setForeground(COL_DEFAULT); break;
        }
        detailBalance.setText("£" + tableModel.getValueAt(row, 5));
        detailCredit.setText("£"  + tableModel.getValueAt(row, 6));
        setDetailEnabled(true);
    }

    // ---------------------------------------------------------------
    // Action dialogs — all wired to DAOs
    // ---------------------------------------------------------------

    /** CA-03: Create new account holder */
    private void openCreateDialog() {
        AccountHolderFormDialog dialog = new AccountHolderFormDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (!dialog.wasConfirmed()) return;

        AccountHolder newHolder = new AccountHolder(
            dialog.getFirstName(), dialog.getLastName(),
            dialog.getEmail(), dialog.getPhone(),
            dialog.getAddress(), dialog.getCity(), dialog.getPostcode()
        );
        try {
            accountHolderDAO.createAccountHolder(newHolder);
            JOptionPane.showMessageDialog(this,
                "Account holder created successfully.", "Created",
                JOptionPane.INFORMATION_MESSAGE);
            loadAllHolders();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    /** CA-05: Edit existing account holder details */
    private void openEditDialog() {
        if (selectedHolderId < 0) return;
        try {
            AccountHolder existing = accountHolderDAO.findById(selectedHolderId);
            if (existing == null) return;
            AccountHolderFormDialog dialog = new AccountHolderFormDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), existing);
            dialog.setVisible(true);
            if (!dialog.wasConfirmed()) return;

            existing.setFirstName(dialog.getFirstName());
            existing.setLastName(dialog.getLastName());
            existing.setEmail(dialog.getEmail());
            existing.setPhone(dialog.getPhone());
            existing.setAddressLine1(dialog.getAddress());
            existing.setCity(dialog.getCity());
            existing.setPostcode(dialog.getPostcode());
            accountHolderDAO.updateAccountHolder(existing);
            JOptionPane.showMessageDialog(this, "Details updated.", "Updated",
                JOptionPane.INFORMATION_MESSAGE);
            loadAllHolders();
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-10: Record a payment from the account holder */
    private void openRecordPaymentDialog() {
        if (selectedHolderId < 0) return;
        String name = detailName.getText();
        String input = JOptionPane.showInputDialog(this,
            "Enter payment amount for " + name + " (£):",
            "Record Payment", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal amount = new BigDecimal(input.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            accountHolderDAO.recordPayment(selectedHolderId, amount);
            JOptionPane.showMessageDialog(this,
                "Payment of £" + String.format("%.2f", amount) + " recorded for " + name,
                "Payment Recorded", JOptionPane.INFORMATION_MESSAGE);
            loadAllHolders();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive amount.",
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-33: Set credit limit */
    private void openSetCreditDialog() {
        if (selectedHolderId < 0) return;
        String name    = detailName.getText();
        String current = detailCredit.getText();
        String input = JOptionPane.showInputDialog(this,
            "Set new credit limit for " + name + " (£):\nCurrent: " + current,
            "Set Credit Limit", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal limit = new BigDecimal(input.trim());
            if (limit.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            accountHolderDAO.setCreditLimit(selectedHolderId, limit);
            JOptionPane.showMessageDialog(this, "Credit limit updated to £" +
                String.format("%.2f", limit), "Updated", JOptionPane.INFORMATION_MESSAGE);
            loadAllHolders();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.",
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-34: Apply discount plan */
    private void openApplyDiscountDialog() {
        if (selectedHolderId < 0) return;
        try {
            String[] planNames = discountPlanDAO.getPlanNamesArray();
            String chosen = (String) JOptionPane.showInputDialog(this,
                "Select discount plan for " + detailName.getText() + ":",
                "Apply Discount Plan", JOptionPane.PLAIN_MESSAGE,
                null, planNames, planNames[0]);
            if (chosen == null) return;
            // Extract plan ID from "1 — No Discount" format
            int planId = Integer.parseInt(chosen.split(" — ")[0].trim());
            accountHolderDAO.applyDiscountPlan(selectedHolderId, planId);
            JOptionPane.showMessageDialog(this, "Discount plan applied.", "Updated",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) { showDbError(ex); }
    }

    /** CA-09: Update account status */
    private void openUpdateStatusDialog() {
        if (selectedHolderId < 0) return;
        String current = detailStatus.getText();
        String[] options = {"NORMAL", "SUSPENDED", "IN_DEFAULT"};
        String chosen = (String) JOptionPane.showInputDialog(this,
            "Update status for " + detailName.getText() + ":\nCurrent: " + current,
            "Update Account Status", JOptionPane.PLAIN_MESSAGE,
            null, options, current);
        if (chosen == null || chosen.equals(current)) return;
        try {
            accountHolderDAO.updateAccountStatus(selectedHolderId, AccountStatus.valueOf(chosen));
            JOptionPane.showMessageDialog(this, "Status updated to " + chosen, "Updated",
                JOptionPane.INFORMATION_MESSAGE);
            loadAllHolders();
        } catch (SQLException ex) { showDbError(ex); }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Runs a DAO call on a background thread, then updates UI on the EDT */
    private void runInBackground(DaoCall<List<AccountHolder>> call,
                                  java.util.function.Consumer<List<AccountHolder>> onDone) {
        SwingWorker<List<AccountHolder>, Void> worker =
            new SwingWorker<List<AccountHolder>, Void>() {
                @Override protected List<AccountHolder> doInBackground() throws Exception {
                    return call.execute();
                }
                @Override protected void done() {
                    try { onDone.accept(get()); }
                    catch (Exception ex) { showDbError(ex); }
                }
            };
        worker.execute();
    }

    @FunctionalInterface
    interface DaoCall<T> { T execute() throws Exception; }

    private void setDetailEnabled(boolean enabled) {
        for (JButton btn : new JButton[]{editBtn, recordPaymentBtn,
                setCreditBtn, applyDiscountBtn, updateStatusBtn})
            btn.setEnabled(enabled);
    }

    private void showDbError(Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel makeDetailRow(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(COL_WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(COL_SUB);
        lbl.setPreferredSize(new Dimension(90, 20));
        row.add(lbl,   BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private JLabel makeDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(COL_TEXT);
        return l;
    }

    private JButton makeActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setBackground(COL_WHITE);
        btn.setBorder(new CompoundBorder(
            new LineBorder(color, 1, true), new EmptyBorder(7, 12, 7, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
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

    private JSeparator makeDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(COL_BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    // ---------------------------------------------------------------
    // Create / Edit dialog (CA-03, CA-05)
    // ---------------------------------------------------------------
    static class AccountHolderFormDialog extends JDialog {
        private boolean confirmed = false;
        private JTextField firstNameField, lastNameField, emailField,
                           phoneField, addressField, cityField, postcodeField;

        AccountHolderFormDialog(JFrame parent, AccountHolder existing) {
            super(parent, existing == null ? "New Account Holder" : "Edit Account Holder", true);
            setSize(420, 500);
            setLocationRelativeTo(parent);
            setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));
            panel.setBackground(Color.WHITE);

            firstNameField = addField(panel, "First Name *",
                existing != null ? existing.getFirstName() : "");
            lastNameField  = addField(panel, "Last Name *",
                existing != null ? existing.getLastName()  : "");
            emailField     = addField(panel, "Email *",
                existing != null ? existing.getEmail()     : "");
            phoneField     = addField(panel, "Phone",
                existing != null ? existing.getPhone()     : "");
            addressField   = addField(panel, "Address Line 1",
                existing != null ? (existing.getAddressLine1() != null ? existing.getAddressLine1() : "") : "");
            cityField      = addField(panel, "City",
                existing != null ? (existing.getCity() != null ? existing.getCity() : "") : "");
            postcodeField  = addField(panel, "Postcode",
                existing != null ? (existing.getPostcode() != null ? existing.getPostcode() : "") : "");

            panel.add(Box.createVerticalStrut(16));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnRow.setBackground(Color.WHITE);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            JButton save = new JButton(existing == null ? "Create" : "Save");
            save.setBackground(new Color(0x1A6B3C));
            save.setForeground(Color.WHITE);
            save.setBorderPainted(false);
            save.addActionListener(e -> {
                if (firstNameField.getText().trim().isEmpty()
                        || lastNameField.getText().trim().isEmpty()
                        || emailField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "First name, last name and email are required.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
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
            panel.add(lbl);
            panel.add(Box.createVerticalStrut(4));
            JTextField f = new JTextField(value);
            f.setFont(new Font("SansSerif", Font.PLAIN, 13));
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD6E4DC), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(f);
            panel.add(Box.createVerticalStrut(10));
            return f;
        }

        boolean wasConfirmed()  { return confirmed; }
        String  getFirstName()  { return firstNameField.getText().trim(); }
        String  getLastName()   { return lastNameField.getText().trim(); }
        String  getEmail()      { return emailField.getText().trim(); }
        String  getPhone()      { return phoneField.getText().trim(); }
        String  getAddress()    { return addressField.getText().trim(); }
        String  getCity()       { return cityField.getText().trim(); }
        String  getPostcode()   { return postcodeField.getText().trim(); }
    }
}
