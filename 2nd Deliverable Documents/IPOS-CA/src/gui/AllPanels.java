package gui;

import dao.*;
import models.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

// =============================================================
// ORDERS PANEL (CA-23, CA-29, CA-30, CA-31, CA-32)
// =============================================================
class OrdersPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);
    private static final Color COL_SUB    = new Color(0x6B7C72);

    private final OrderDAO orderDAO = new OrderDAO();
    private DefaultTableModel ordersModel;
    private JLabel balanceLabel, pendingLabel, totalLabel;

    public OrdersPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(COL_BG);
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildSummary(), BorderLayout.SOUTH);
        loadOrders();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COL_BG);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));
        JLabel info = new JLabel("Orders placed with InfoPharma (IPOS-SA). Double-click to view items.");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(COL_SUB);
        JButton newBtn = makeButton("+ Place New Order", COL_PRI, Color.WHITE);
        newBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Cross-system order form: reads IPOS-SA catalogue via shared DB.\n" +
            "Coordinate table names with Team A to complete this.", "Place Order",
            JOptionPane.INFORMATION_MESSAGE));
        bar.add(info, BorderLayout.WEST);
        bar.add(newBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);
        String[] cols = {"Order Ref","Date Placed","Total (£)","Status",
                         "Dispatched","Delivered","Payment"};
        ordersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(ordersModel);
        styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                l.setFont(new Font("SansSerif", Font.BOLD, 12));
                if (!sel) switch (v == null ? "" : v.toString()) {
                    case "DELIVERED":  l.setForeground(new Color(0x22C55E)); break;
                    case "DISPATCHED": l.setForeground(new Color(0x3B82F6)); break;
                    case "ACCEPTED":   l.setForeground(new Color(0x8B5CF6)); break;
                    default:           l.setForeground(new Color(0xF59E0B));
                }
                return l;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummary() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        bar.setBackground(new Color(0xF1F5F9));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, COL_BORDER));
        totalLabel   = makeSumLabel("Total Orders: —");
        balanceLabel = makeSumLabel("Outstanding Balance: —");
        pendingLabel = makeSumLabel("Pending Delivery: —");
        bar.add(totalLabel); bar.add(balanceLabel); bar.add(pendingLabel);
        return bar;
    }

    private void loadOrders() {
        SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            List<models.Order> orders;
            BigDecimal balance;
            @Override protected Void doInBackground() throws Exception {
                orders  = orderDAO.getAllOrders();
                balance = orderDAO.getTotalOutstandingBalance();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    ordersModel.setRowCount(0);
                    int pending = 0;
                    for (models.Order o : orders) {
                        String dispatched = o.getDispatchDate() != null
                            ? o.getDispatchDate().toLocalDateTime().toLocalDate().toString() : "—";
                        String delivered  = o.getDeliveryDate() != null
                            ? o.getDeliveryDate().toLocalDateTime().toLocalDate().toString() : "—";
                        ordersModel.addRow(new Object[]{
                            o.getIposSaOrderRef() != null ? o.getIposSaOrderRef() : "IP-" + o.getOrderId(),
                            o.getOrderDate().toLocalDateTime().toLocalDate().toString(),
                            String.format("%.2f", o.getTotalAmount()),
                            o.getStatus(), dispatched, delivered, o.getPaymentStatus()
                        });
                        if (!"DELIVERED".equals(o.getStatus())) pending++;
                    }
                    totalLabel.setText("Total Orders: " + orders.size());
                    balanceLabel.setText("Outstanding Balance: £" + String.format("%.2f", balance));
                    pendingLabel.setText("Pending Delivery: " + pending);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OrdersPanel.this,
                        "DB error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private JLabel makeSumLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(COL_SUB); return l;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13)); t.setRowHeight(34);
        t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0,0));
        t.setBackground(COL_WHITE); t.setSelectionBackground(new Color(0xE8F5EE));
        t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(0xF1F5F9));
    }

    private JButton makeButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t); b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8,16,8,16)); return b;
    }
}


// =============================================================
// STATEMENTS PANEL (CA-07, CA-11, CA-12)
// =============================================================
class StatementsPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);

    private final AccountHolderDAO accountHolderDAO = new AccountHolderDAO();
    private final ReminderDAO      reminderDAO      = new ReminderDAO();

    private DefaultTableModel model;
    private JTable            table;

    public StatementsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(COL_BG);
        add(buildInfoBanner(), BorderLayout.NORTH);
        add(buildTable(),      BorderLayout.CENTER);
        add(buildActionBar(),  BorderLayout.SOUTH);
        loadDueReminders();
    }

    private JPanel buildInfoBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(0xFEF3C7));
        banner.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xFCD34D),1,true), new EmptyBorder(12,16,12,16)));
        JLabel lbl = new JLabel(
            "⚠  Account holders with outstanding balances and due reminders are listed below.");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(new Color(0x92400E));
        banner.add(lbl);
        return banner;
    }

    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);
        String[] cols = {"ID","Name","Amount Owed (£)","Reminder Type","Payment Due By"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(34); table.setShowGrid(false);
        table.setBackground(COL_WHITE); table.setSelectionBackground(new Color(0xE8F5EE));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF1F5F9));

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0)
                    markReminderSent(table.getSelectedRow());
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(COL_BG);
        JButton refresh = makeButton("↺ Refresh", new Color(0xE8F5EE), COL_PRI);
        refresh.setBorder(new CompoundBorder(new LineBorder(COL_BORDER,1,true), new EmptyBorder(7,14,7,14)));
        refresh.addActionListener(e -> loadDueReminders());
        bar.add(refresh);
        JLabel hint = new JLabel("   Double-click a row to mark reminder as sent.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(0x999999));
        bar.add(hint);
        return bar;
    }

    private void loadDueReminders() {
        SwingWorker<List<Object[]>, Void> w = new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return reminderDAO.getUnsentReminders();
            }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    for (Object[] row : get()) {
                        model.addRow(new Object[]{
                            row[1], row[2], String.format("%.2f", row[5]),
                            row[3], row[4]
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StatementsPanel.this, "DB error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void markReminderSent(int row) {
        // The model row maps to the reminder; reload to get reminder IDs
        int confirm = JOptionPane.showConfirmDialog(this,
            "Mark this reminder as sent for " + model.getValueAt(row, 1) + "?",
            "Mark Sent", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        JOptionPane.showMessageDialog(this,
            "Reminder marked as sent.\n(Full implementation: reminderDAO.markAsSent(reminderId))",
            "Done", JOptionPane.INFORMATION_MESSAGE);
        loadDueReminders();
    }

    private JButton makeButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t); b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8,16,8,16)); return b;
    }
}


// =============================================================
// USER MANAGEMENT PANEL (CA-03, CA-04, CA-08) — Admin only
// =============================================================
class UserManagementPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);

    private final UserDAO userDAO = new UserDAO();
    private DefaultTableModel model;
    private JTable            table;

    public UserManagementPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(COL_BG);
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTable(),  BorderLayout.CENTER);
        loadUsers();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COL_BG);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));
        JLabel note = new JLabel("Right-click a row to change role or deactivate. Admin access only.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        note.setForeground(new Color(0x6B7C72));
        JButton addBtn = makeButton("+ Add User", COL_PRI, Color.WHITE);
        addBtn.addActionListener(e -> openAddUserDialog());
        bar.add(note, BorderLayout.WEST);
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);
        String[] cols = {"ID","Username","Full Name","Role","Active"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(34); table.setShowGrid(false);
        table.setBackground(COL_WHITE); table.setSelectionBackground(new Color(0xE8F5EE));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF1F5F9));

        // Colour the Active column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                if (!sel) {
                    l.setBackground(COL_WHITE);
                    l.setForeground("✅".equals(v) ? new Color(0x22C55E) : new Color(0xEF4444));
                }
                return l;
            }
        });

        JPopupMenu menu = new JPopupMenu();
        JMenuItem changeRole   = new JMenuItem("Change Role");
        JMenuItem deactivate   = new JMenuItem("Deactivate Account");
        JMenuItem reactivate   = new JMenuItem("Reactivate Account");
        changeRole.addActionListener(e -> changeSelectedUserRole());
        deactivate.addActionListener(e -> deactivateSelectedUser());
        reactivate.addActionListener(e -> reactivateSelectedUser());

        // Show relevant menu items based on current active status
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                boolean isActive = "✅".equals(model.getValueAt(row, 4));
                changeRole.setVisible(isActive);
                deactivate.setVisible(isActive);
                reactivate.setVisible(!isActive);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        menu.add(changeRole);
        menu.add(deactivate);
        menu.add(reactivate);
        table.setComponentPopupMenu(menu);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Right-click a user to change role, deactivate, or reactivate.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(0xAAAAAA));
        hint.setBorder(new EmptyBorder(6, 4, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    // Now loads ALL users including inactive (ordered by ID)
    private void loadUsers() {
        SwingWorker<List<models.User>, Void> w = new SwingWorker<List<models.User>, Void>() {
            @Override protected List<models.User> doInBackground() throws Exception {
                return userDAO.getAllUsers(); // all users, not just active
            }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    for (models.User u : get()) {
                        model.addRow(new Object[]{
                            u.getUserId(), u.getUsername(), u.getFullName(),
                            u.getRole().name(), u.isActive() ? "✅" : "❌"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "DB error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    /** CA-08: Change role */
    private void changeSelectedUserRole() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int userId = (int) model.getValueAt(row, 0);
        String current = (String) model.getValueAt(row, 3);
        String[] roles = {"ADMIN","PHARMACIST","MANAGER"};
        String chosen = (String) JOptionPane.showInputDialog(this,
            "Select new role:", "Change Role", JOptionPane.PLAIN_MESSAGE,
            null, roles, current);
        if (chosen == null || chosen.equals(current)) return;
        try {
            userDAO.updateUserRole(userId, models.User.Role.valueOf(chosen));
            loadUsers();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    /** CA-04: Deactivate account */
    private void deactivateSelectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int userId = (int) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Deactivate account for " + name + "?\nThey will no longer be able to log in.",
            "Confirm Deactivate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            userDAO.deactivateUser(userId);
            loadUsers();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    /** Reactivate a previously deactivated account */
    private void reactivateSelectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int userId = (int) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reactivate account for " + name + "?\nThey will be able to log in again.",
            "Confirm Reactivate", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            userDAO.reactivateUser(userId);
            loadUsers();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    /** CA-03: Add new staff user */
    private void openAddUserDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
            "Add Staff User", true);
        dialog.setSize(360, 380);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        String[] labels = {"Username *","Password *","First Name *","Last Name *"};
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lbl); panel.add(Box.createVerticalStrut(4));
            fields[i] = new JTextField();
            fields[i].setFont(new Font("SansSerif", Font.PLAIN, 13));
            fields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            fields[i].setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD6E4DC),1,true), new EmptyBorder(5,8,5,8)));
            fields[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(fields[i]); panel.add(Box.createVerticalStrut(8));
        }

        String[] roles = {"PHARMACIST","MANAGER","ADMIN"};
        JComboBox<String> roleBox = new JComboBox<>(roles);
        roleBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        roleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel roleLbl = new JLabel("Role *");
        roleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(roleLbl); panel.add(Box.createVerticalStrut(4));
        panel.add(roleBox); panel.add(Box.createVerticalStrut(16));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setBackground(Color.WHITE);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        JButton create = new JButton("Create User");
        create.setBackground(COL_PRI); create.setForeground(Color.WHITE);
        create.setBorderPainted(false);
        create.addActionListener(e -> {
            if (fields[0].getText().trim().isEmpty() || fields[1].getText().trim().isEmpty()
                    || fields[2].getText().trim().isEmpty() || fields[3].getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.");
                return;
            }
            try {
                models.User newUser = new models.User(
                    fields[0].getText().trim(), fields[1].getText().trim(),
                    fields[2].getText().trim(), fields[3].getText().trim(),
                    models.User.Role.valueOf((String) roleBox.getSelectedItem())
                );
                userDAO.createUser(newUser);
                dialog.dispose();
                loadUsers();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "DB error: " + ex.getMessage());
            }
        });
        btnRow.add(cancel); btnRow.add(create);
        panel.add(btnRow);
        dialog.setContentPane(new JScrollPane(panel));
        dialog.setVisible(true);
    }

    private JButton makeButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t); b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8,16,8,16)); return b;
    }
}


// =============================================================
// REPORTS PANEL (CA-24, CA-25, CA-26, CA-27, CA-28)
// =============================================================
class ReportsPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);

    private final ReportDAO           reportDAO           = new ReportDAO();
    private final MerchantSettingsDAO merchantSettingsDAO = new MerchantSettingsDAO();

    private JComboBox<String> reportTypeBox;
    private JTextField        fromField, toField;
    private JPanel            resultsArea;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(COL_BG);
        add(buildControls(), BorderLayout.NORTH);
        add(buildResults(),  BorderLayout.CENTER);
    }

    private JPanel buildControls() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(COL_WHITE);
        wrapper.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(14, 16, 14, 16)));

        // Row 1 — report type + date range
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setBackground(COL_WHITE);

        row1.add(lbl("Report Type:"));
        reportTypeBox = new JComboBox<>(new String[]{
            "Turnover Report (CA-26)",
            "Stock Availability Report (CA-27)",
            "Account Holder Debt Report (CA-28)"
        });
        reportTypeBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        reportTypeBox.setPreferredSize(new Dimension(240, 34));
        row1.add(reportTypeBox);

        row1.add(Box.createHorizontalStrut(12));
        row1.add(lbl("From:"));
        fromField = dateField("01/01/2026");
        row1.add(fromField);

        row1.add(lbl("To:"));
        toField = dateField("31/03/2026");
        row1.add(toField);

        // Row 2 — action buttons pinned to the right so they never get cut off
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row2.setBackground(COL_WHITE);

        JButton genBtn = makeButton("Generate Report", COL_PRI, Color.WHITE);
        genBtn.addActionListener(e -> generateReport());

        JButton printBtn = new JButton("Print");
        printBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        printBtn.setForeground(COL_PRI);
        printBtn.setBackground(new Color(0xE8F5EE));
        printBtn.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(7, 14, 7, 14)));
        printBtn.setFocusPainted(false);
        printBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        printBtn.addActionListener(e -> printResults());

        row2.add(genBtn);
        row2.add(printBtn);

        wrapper.add(row1, BorderLayout.CENTER);
        wrapper.add(row2, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildResults() {
        resultsArea = new JPanel(new BorderLayout());
        resultsArea.setBackground(COL_WHITE);
        resultsArea.setBorder(new LineBorder(COL_BORDER, 1, true));
        JLabel placeholder = new JLabel("Select a report type and date range, then click Generate.");
        placeholder.setFont(new Font("SansSerif", Font.ITALIC, 13));
        placeholder.setForeground(new Color(0x6B7C72));
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        resultsArea.add(placeholder, BorderLayout.CENTER);
        return resultsArea;
    }

    private void generateReport() {
        String type = (String) reportTypeBox.getSelectedItem();
        Timestamp from = parseDate(fromField.getText() + " 00:00:00");
        Timestamp to   = parseDate(toField.getText()   + " 23:59:59");

        resultsArea.removeAll();
        resultsArea.add(new JLabel("Loading...", SwingConstants.CENTER), BorderLayout.CENTER);
        resultsArea.revalidate(); resultsArea.repaint();

        if (type != null && type.contains("Turnover")) {
            SwingWorker<List<Object[]>, Void> w = new SwingWorker<List<Object[]>, Void>() {
                @Override protected List<Object[]> doInBackground() throws Exception {
                    return reportDAO.getTurnoverReport(from, to);
                }
                @Override protected void done() {
                    try { showTable(new String[]{"Invoice","Date","Customer","Subtotal","VAT","Discount","Total","Method"}, get()); }
                    catch (Exception ex) { showError(ex); }
                }
            };
            w.execute();
        } else if (type != null && type.contains("Stock")) {
            SwingWorker<List<Object[]>, Void> w = new SwingWorker<List<Object[]>, Void>() {
                @Override protected List<Object[]> doInBackground() throws Exception {
                    BigDecimal vat = merchantSettingsDAO.getVatRate();
                    return reportDAO.getStockReport(vat.doubleValue());
                }
                @Override protected void done() {
                    try { showTable(new String[]{"Description","Qty","Min Level","Bulk Cost","Markup","Retail Price","Status"}, get()); }
                    catch (Exception ex) { showError(ex); }
                }
            };
            w.execute();
        } else {
            SwingWorker<List<Object[]>, Void> w = new SwingWorker<List<Object[]>, Void>() {
                @Override protected List<Object[]> doInBackground() throws Exception {
                    return reportDAO.getDebtReport(from, to);
                }
                @Override protected void done() {
                    try { showTable(new String[]{"Name","Opening Balance","Payments Received","New Charges","Closing Balance","Status"}, get()); }
                    catch (Exception ex) { showError(ex); }
                }
            };
            w.execute();
        }
    }

    private void showTable(String[] cols, List<Object[]> rows) {
        DefaultTableModel m = new DefaultTableModel(cols, 0);
        for (Object[] row : rows) m.addRow(row);
        JTable t = new JTable(m);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13)); t.setRowHeight(30);
        t.setShowGrid(false); t.setBackground(COL_WHITE); t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(0xF1F5F9));
        resultsArea.removeAll();
        resultsArea.add(new JScrollPane(t), BorderLayout.CENTER);
        resultsArea.revalidate(); resultsArea.repaint();
    }

    private void showError(Exception ex) {
        resultsArea.removeAll();
        JLabel err = new JLabel("Error: " + ex.getMessage(), SwingConstants.CENTER);
        err.setForeground(new Color(0xEF4444));
        resultsArea.add(err, BorderLayout.CENTER);
        resultsArea.revalidate(); resultsArea.repaint();
    }

    private void printResults() {
        Component comp = resultsArea.getComponent(0);
        if (comp instanceof JScrollPane) {
            JTable t = (JTable) ((JScrollPane) comp).getViewport().getView();
            try { t.print(); } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Generate a report first.");
        }
    }

    private Timestamp parseDate(String s) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return new Timestamp(sdf.parse(s).getTime());
        } catch (Exception e) { return new Timestamp(System.currentTimeMillis()); }
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(new Color(0x6B7C72)); return l;
    }

    private JTextField dateField(String def) {
        JTextField f = new JTextField(def);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(100, 34));
        f.setBorder(new CompoundBorder(new LineBorder(COL_BORDER,1,true), new EmptyBorder(6,8,6,8)));
        return f;
    }

    private JButton makeButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t); b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8,16,8,16)); return b;
    }
}


// =============================================================
// CREDIT & DISCOUNTS PANEL (CA-33, CA-34)
// =============================================================
class CreditDiscountsPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);

    private final AccountHolderDAO accountHolderDAO = new AccountHolderDAO();
    private final DiscountPlanDAO  discountPlanDAO  = new DiscountPlanDAO();
    private DefaultTableModel creditModel;

    public CreditDiscountsPanel() {
        setLayout(new GridLayout(1, 2, 16, 0));
        setBackground(COL_BG);
        add(buildCreditPanel());
        add(buildDiscountPanel());
    }

    private JPanel buildCreditPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(16,16,16,16)));
        JLabel heading = new JLabel("Set Credit Limits (CA-33)");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));
        panel.add(heading, BorderLayout.NORTH);

        String[] cols = {"ID","Name","Credit Limit (£)","Balance (£)","Status"};
        creditModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(creditModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13)); table.setRowHeight(30);
        table.setShowGrid(false); table.setBackground(COL_WHITE); table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF1F5F9));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(COL_BORDER,1,true));
        panel.add(scroll, BorderLayout.CENTER);

        JButton btn = new JButton("Set Credit Limit for Selected");
        btn.setBackground(COL_PRI); btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select an account holder."); return; }
            int holderId = (int) creditModel.getValueAt(row, 0);
            String name  = (String) creditModel.getValueAt(row, 1);
            String input = JOptionPane.showInputDialog(this,
                "New credit limit for " + name + " (£):");
            if (input == null || input.trim().isEmpty()) return;
            try {
                BigDecimal limit = new BigDecimal(input.trim());
                accountHolderDAO.setCreditLimit(holderId, limit);
                loadCreditData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        panel.add(btn, BorderLayout.SOUTH);
        loadCreditData();
        return panel;
    }

    private void loadCreditData() {
        SwingWorker<java.util.List<models.AccountHolder>, Void> w =
            new SwingWorker<java.util.List<models.AccountHolder>, Void>() {
                @Override protected java.util.List<models.AccountHolder> doInBackground() throws Exception {
                    return accountHolderDAO.getAllAccountHolders();
                }
                @Override protected void done() {
                    try {
                        creditModel.setRowCount(0);
                        for (models.AccountHolder ah : get()) {
                            creditModel.addRow(new Object[]{
                                ah.getHolderId(), ah.getFullName(),
                                String.format("%.2f", ah.getCreditLimit()),
                                String.format("%.2f", ah.getOutstandingBalance()),
                                ah.getAccountStatus().name()
                            });
                        }
                    } catch (Exception ex) { /* silent */ }
                }
            };
        w.execute();
    }

    private JPanel buildDiscountPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(16,16,16,16)));
        JLabel heading = new JLabel("Discount Plans (CA-34)");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));
        panel.add(heading, BorderLayout.NORTH);

        JPanel plansPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        plansPanel.setBackground(COL_WHITE);

        SwingWorker<java.util.List<Object[]>, Void> w =
            new SwingWorker<java.util.List<Object[]>, Void>() {
                @Override protected java.util.List<Object[]> doInBackground() throws Exception {
                    return discountPlanDAO.getAllPlans();
                }
                @Override protected void done() {
                    try {
                        for (Object[] p : get()) {
                            JPanel card = new JPanel(new BorderLayout());
                            card.setBackground(new Color(0xF8FAFC));
                            card.setBorder(new CompoundBorder(
                                new LineBorder(COL_BORDER,1,true), new EmptyBorder(10,14,10,14)));
                            JLabel name = new JLabel((String) p[1]);
                            name.setFont(new Font("SansSerif", Font.BOLD, 13));
                            JLabel detail = new JLabel(p[2] + " — " + p[3]);
                            detail.setFont(new Font("SansSerif", Font.PLAIN, 11));
                            detail.setForeground(new Color(0x6B7C72));
                            JPanel text = new JPanel(new GridLayout(2, 1));
                            text.setBackground(new Color(0xF8FAFC));
                            text.add(name); text.add(detail);
                            card.add(text, BorderLayout.CENTER);
                            plansPanel.add(card);
                        }
                        plansPanel.revalidate(); plansPanel.repaint();
                    } catch (Exception ex) { /* silent */ }
                }
            };
        w.execute();

        JScrollPane scroll = new JScrollPane(plansPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Assign plans in the Account Holders panel.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(0x6B7C72));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }
}


// =============================================================
// TEMPLATES PANEL (CA-35, CA-36)
// =============================================================
class TemplatesPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);

    private final TemplateDAO templateDAO = new TemplateDAO();

    public TemplatesPanel() {
        setLayout(new BorderLayout());
        setBackground(COL_BG);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("1st Reminder (CA-35)",   buildEditor(TemplateDAO.FIRST_REMINDER));
        tabs.addTab("2nd Reminder (CA-35)",   buildEditor(TemplateDAO.SECOND_REMINDER));
        tabs.addTab("Receipt/Invoice (CA-36)", buildEditor(TemplateDAO.RECEIPT));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildEditor(String type) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel hint = new JLabel(
            "Placeholders: {HOLDER_NAME}  {INVOICE_NO}  {ACCOUNT_NO}  {AMOUNT}  {PAYMENT_DUE_DATE}  {PHARMACIST_NAME}");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(0x6B7C72));
        hint.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(8,12,8,12)));

        JTextArea editor = new JTextArea("Loading...");
        editor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        editor.setLineWrap(true); editor.setWrapStyleWord(true);
        editor.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Load from DB
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return templateDAO.getTemplate(type);
            }
            @Override protected void done() {
                try {
                    String body = get();
                    editor.setText(body != null ? body : "(Template not found in database)");
                } catch (Exception ignored) {}
            }
        };
        w.execute();

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setBackground(COL_WHITE);
        JButton reload = new JButton("Reload from DB");
        reload.addActionListener(e -> w.execute());
        JButton save = new JButton("Save Template");
        save.setBackground(COL_PRI); save.setForeground(Color.WHITE);
        save.setBorderPainted(false);
        save.addActionListener(e -> {
            try {
                templateDAO.saveTemplate(type, editor.getText());
                JOptionPane.showMessageDialog(panel, "Template saved.", "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "DB error: " + ex.getMessage());
            }
        });
        btnRow.add(reload); btnRow.add(save);

        panel.add(hint,    BorderLayout.NORTH);
        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(btnRow,  BorderLayout.SOUTH);
        return panel;
    }
}


// =============================================================
// SETTINGS PANEL (CA-21, CA-37)
// =============================================================
class SettingsPanel extends JPanel {

    private static final Color COL_BG     = new Color(0xF5F7FA);
    private static final Color COL_WHITE  = Color.WHITE;
    private static final Color COL_PRI    = new Color(0x1A6B3C);
    private static final Color COL_BORDER = new Color(0xD6E4DC);
    private static final Color COL_TEXT   = new Color(0x1C2B20);

    private final MerchantSettingsDAO merchantSettingsDAO = new MerchantSettingsDAO();

    private JTextField[] identityFields;
    private JTextField   vatField;

    public SettingsPanel() {
        setLayout(new GridBagLayout());
        setBackground(COL_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1; gc.weighty = 1;
        gc.insets = new Insets(0, 0, 0, 12);
        gc.gridx = 0; add(buildIdentityPanel(), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 0, 0);
        add(buildVatPanel(), gc);
        loadSettings();
    }

    private JPanel buildIdentityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(20,20,20,20)));

        JLabel heading = new JLabel("Merchant Identity (CA-37)");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));
        heading.setForeground(COL_TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading); panel.add(Box.createVerticalStrut(16));

        String[] labels = {"Pharmacy Name","Address Line 1","Address Line 2",
                           "City","Postcode","Phone","Fax","Email"};
        identityFields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i] + ":");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lbl); panel.add(Box.createVerticalStrut(4));
            identityFields[i] = new JTextField();
            identityFields[i].setFont(new Font("SansSerif", Font.PLAIN, 13));
            identityFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            identityFields[i].setBorder(new CompoundBorder(
                new LineBorder(COL_BORDER,1,true), new EmptyBorder(6,10,6,10)));
            identityFields[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(identityFields[i]); panel.add(Box.createVerticalStrut(10));
        }

        panel.add(Box.createVerticalGlue());
        JButton save = new JButton("Save Identity Details");
        save.setBackground(COL_PRI); save.setForeground(Color.WHITE);
        save.setBorderPainted(false); save.setFocusPainted(false);
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> saveIdentity());
        panel.add(save);
        return panel;
    }

    private JPanel buildVatPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(20,20,20,20)));

        JLabel heading = new JLabel("VAT Configuration (CA-21)");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));
        heading.setForeground(COL_TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading); panel.add(Box.createVerticalStrut(8));

        JLabel sub = new JLabel("Applied to all retail sales. Set to 0% to disable.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(0x6B7C72));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sub); panel.add(Box.createVerticalStrut(20));

        JLabel rateLbl = new JLabel("Current VAT Rate (%):");
        rateLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        rateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rateLbl); panel.add(Box.createVerticalStrut(6));

        vatField = new JTextField("20.00");
        vatField.setFont(new Font("Georgia", Font.BOLD, 28));
        vatField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        vatField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(8,14,8,14)));
        vatField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(vatField); panel.add(Box.createVerticalStrut(16));

        JLabel note = new JLabel(
            "<html><i>Changing this affects all future retail<br>price calculations immediately.</i></html>");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(new Color(0x92400E));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(note); panel.add(Box.createVerticalGlue());

        JButton save = new JButton("Save VAT Rate");
        save.setBackground(COL_PRI); save.setForeground(Color.WHITE);
        save.setBorderPainted(false); save.setFocusPainted(false);
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> saveVatRate());
        panel.add(save);
        return panel;
    }

    private void loadSettings() {
        SwingWorker<String[], Void> w = new SwingWorker<String[], Void>() {
            @Override protected String[] doInBackground() throws Exception {
                return merchantSettingsDAO.getSettings();
            }
            @Override protected void done() {
                try {
                    String[] settings = get();
                    if (settings == null) return;
                    for (int i = 0; i < Math.min(identityFields.length, settings.length - 1); i++) {
                        identityFields[i].setText(settings[i] != null ? settings[i] : "");
                    }
                    vatField.setText(settings[9] != null ? settings[9] : "20.00");
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    /** CA-37 */
    private void saveIdentity() {
        try {
            merchantSettingsDAO.updateIdentity(
                identityFields[0].getText().trim(),
                identityFields[1].getText().trim(),
                identityFields[2].getText().trim(),
                identityFields[3].getText().trim(),
                identityFields[4].getText().trim(),
                identityFields[5].getText().trim(),
                identityFields[6].getText().trim(),
                identityFields[7].getText().trim()
            );
            JOptionPane.showMessageDialog(this, "Pharmacy identity saved.", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    /** CA-21 */
    private void saveVatRate() {
        try {
            BigDecimal rate = new BigDecimal(vatField.getText().trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0)
                throw new NumberFormatException();
            merchantSettingsDAO.updateVatRate(rate);
            JOptionPane.showMessageDialog(this, "VAT rate saved as " + rate + "%", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid rate (0-100).", "Invalid",
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }
}
