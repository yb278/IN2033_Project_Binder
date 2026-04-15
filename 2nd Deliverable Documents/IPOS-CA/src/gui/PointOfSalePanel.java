package gui;

import dao.AccountHolderDAO;
import dao.DiscountPlanDAO;
import dao.MerchantSettingsDAO;
import dao.SalesDAO;
import dao.StockDAO;
import models.AccountHolder;
import models.Sale;
import models.Sale.PaymentMethod;
import models.SaleItem;
import models.StockItem;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PointOfSalePanel
 *
 * Covers: CA-13 Record Sale, CA-14 Accept Payment,
 *         CA-15 Cash Payment, CA-16 Card Payment,
 *         CA-17 Generate Receipt/Invoice
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class PointOfSalePanel extends JPanel {

    private static final Color COL_BG      = new Color(0xF5F7FA);
    private static final Color COL_WHITE   = Color.WHITE;
    private static final Color COL_PRIMARY = new Color(0x1A6B3C);
    private static final Color COL_BORDER  = new Color(0xD6E4DC);
    private static final Color COL_TEXT    = new Color(0x1C2B20);
    private static final Color COL_SUB     = new Color(0x6B7C72);

    private final SalesDAO            salesDAO            = new SalesDAO();
    private final StockDAO            stockDAO            = new StockDAO();
    private final AccountHolderDAO    accountHolderDAO    = new AccountHolderDAO();
    private final DiscountPlanDAO     discountPlanDAO     = new DiscountPlanDAO();
    private final MerchantSettingsDAO merchantSettingsDAO = new MerchantSettingsDAO();

    // Basket state
    private DefaultTableModel         basketModel;
    private JTable                    basketTable;
    private List<StockItem>           stockItems    = new ArrayList<>();
    private List<StockItem>           basketStockItems = new ArrayList<>();
    private List<Integer>             basketQuantities = new ArrayList<>();
    private AccountHolder             selectedHolder = null;
    private BigDecimal                vatRate       = new BigDecimal("20.00");

    // Totals labels
    private JLabel subtotalLabel, vatLabel, discountLabel, totalLabel, changeLabel;

    // Product search (left)
    private JTextField        productSearchField;
    private DefaultListModel<String> productListModel;
    private JList<String>     productList;
    private JSpinner          quantitySpinner;

    // Customer (right)
    private JRadioButton      walkInRadio, accountRadio;
    private JTextField        customerSearchField;
    private JLabel            customerNameLabel, customerStatusLabel;

    // Payment (right)
    private JComboBox<String> paymentMethodBox;
    private JTextField        amountTenderedField;
    private JTextField        cardTypeField, cardFirst4Field, cardLast4Field, cardExpiryField;
    private JPanel            cardDetailsPanel;
    private JButton           checkoutBtn;

    // The logged-in user ID — set by MainFrame when creating this panel
    private int servedByUserId = 1;

    public PointOfSalePanel() {
        setLayout(new BorderLayout(12, 0));
        setBackground(COL_BG);
        add(buildProductSearch(), BorderLayout.WEST);
        add(buildBasket(),        BorderLayout.CENTER);
        add(buildCheckout(),      BorderLayout.EAST);
        loadStockItems();
        loadVatRate();
    }

    /** Called by MainFrame to pass the logged-in user's ID */
    public void setServedByUserId(int userId) { this.servedByUserId = userId; }

    // ---------------------------------------------------------------
    // LEFT — product search
    // ---------------------------------------------------------------
    private JPanel buildProductSearch() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        panel.setPreferredSize(new Dimension(250, 0));

        JLabel heading = new JLabel("Add Items");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));
        panel.add(heading, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setBackground(COL_WHITE);

        productSearchField = new JTextField();
        productSearchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        productSearchField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(7, 10, 7, 10)));
        productSearchField.setForeground(COL_SUB);
        productSearchField.setText("Search products...");
        productSearchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (productSearchField.getText().startsWith("Search")) {
                    productSearchField.setText(""); productSearchField.setForeground(COL_TEXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (productSearchField.getText().isEmpty()) {
                    productSearchField.setForeground(COL_SUB);
                    productSearchField.setText("Search products...");
                }
            }
        });
        productSearchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterProducts(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterProducts(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}
            });

        productListModel = new DefaultListModel<>();
        productList      = new JList<>(productListModel);
        productList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        productList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productList.setFixedCellHeight(28);

        JScrollPane listScroll = new JScrollPane(productList);
        listScroll.setBorder(new LineBorder(COL_BORDER, 1, true));

        JPanel qtyRow = new JPanel(new BorderLayout(8, 0));
        qtyRow.setBackground(COL_WHITE);
        JLabel qtyLbl = new JLabel("Quantity:");
        qtyLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        quantitySpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        qtyRow.add(qtyLbl, BorderLayout.WEST);
        qtyRow.add(quantitySpinner, BorderLayout.CENTER);

        centerPanel.add(productSearchField, BorderLayout.NORTH);
        centerPanel.add(listScroll,         BorderLayout.CENTER);
        centerPanel.add(qtyRow,             BorderLayout.SOUTH);

        JButton addBtn = new JButton("Add to Basket →");
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        addBtn.setBackground(COL_PRIMARY); addBtn.setForeground(Color.WHITE);
        addBtn.setBorderPainted(false); addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> addSelectedToBasket());

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(addBtn,      BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------
    // CENTER — basket
    // ---------------------------------------------------------------
    private JPanel buildBasket() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));

        JLabel heading = new JLabel("Current Sale");
        heading.setFont(new Font("Georgia", Font.BOLD, 14));

        String[] cols = {"Description", "Qty", "Unit Price (£)", "Line Total (£)"};
        basketModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        basketTable = new JTable(basketModel);
        basketTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        basketTable.setRowHeight(32);
        basketTable.setShowGrid(false);
        basketTable.setIntercellSpacing(new Dimension(0,0));
        basketTable.setBackground(COL_WHITE);
        basketTable.setSelectionBackground(new Color(0x4F9E6F)); basketTable.setSelectionForeground(Color.WHITE);
        basketTable.setFillsViewportHeight(true);
        basketTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        basketTable.getTableHeader().setBackground(new Color(0xF1F5F9));

        int[] widths = {220, 60, 130, 130};
        for (int i = 0; i < widths.length; i++)
            basketTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(basketTable);
        scroll.setBorder(new LineBorder(COL_BORDER, 1, true));

        JButton removeBtn = new JButton("Remove Selected Item");
        removeBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        removeBtn.setForeground(new Color(0xEF4444));
        removeBtn.setBackground(COL_WHITE);
        removeBtn.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xEF4444),1,true), new EmptyBorder(6,12,6,12)));
        removeBtn.setFocusPainted(false);
        removeBtn.addActionListener(e -> removeFromBasket());

        // Totals
        JPanel totalsPanel = new JPanel(new GridLayout(4, 2, 8, 4));
        totalsPanel.setBackground(new Color(0xF8FAFC));
        totalsPanel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(12,16,12,16)));

        subtotalLabel = makeTotalLabel("£0.00");
        vatLabel      = makeTotalLabel("£0.00");
        discountLabel = makeTotalLabel("£0.00");
        totalLabel    = makeTotalLabel("£0.00");
        totalLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        totalLabel.setForeground(COL_PRIMARY);

        totalsPanel.add(makeTotalKey("Subtotal:"));   totalsPanel.add(subtotalLabel);
        totalsPanel.add(makeTotalKey("VAT (" + vatRate + "%):")); totalsPanel.add(vatLabel);
        totalsPanel.add(makeTotalKey("Discount:"));   totalsPanel.add(discountLabel);
        totalsPanel.add(makeTotalKey("TOTAL DUE:"));  totalsPanel.add(totalLabel);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setBackground(COL_WHITE);
        bottom.add(removeBtn,   BorderLayout.NORTH);
        bottom.add(totalsPanel, BorderLayout.CENTER);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(bottom,  BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------
    // RIGHT — checkout
    // ---------------------------------------------------------------
    private JPanel buildCheckout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COL_WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(16,16,16,16)));
        panel.setPreferredSize(new Dimension(240, 0));

        // Customer section
        addSectionHeading(panel, "Customer");
        panel.add(Box.createVerticalStrut(10));

        ButtonGroup g = new ButtonGroup();
        walkInRadio  = new JRadioButton("Walk-in customer", true);
        accountRadio = new JRadioButton("Account holder");
        for (JRadioButton r : new JRadioButton[]{walkInRadio, accountRadio}) {
            r.setBackground(COL_WHITE); r.setFont(new Font("SansSerif", Font.PLAIN, 12));
            r.setAlignmentX(Component.LEFT_ALIGNMENT); g.add(r); panel.add(r);
        }
        panel.add(Box.createVerticalStrut(8));

        customerSearchField = new JTextField();
        customerSearchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        customerSearchField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(6,8,6,8)));
        customerSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        customerSearchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        customerSearchField.setEnabled(false);
        customerSearchField.setForeground(COL_SUB);
        customerSearchField.setText("Enter account holder ID...");
        customerSearchField.addActionListener(e -> lookupAccountHolder());
        // Clear placeholder text when clicked/focused
        customerSearchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (customerSearchField.getText().equals("Enter account holder ID...")) {
                    customerSearchField.setText("");
                    customerSearchField.setForeground(COL_TEXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (customerSearchField.getText().trim().isEmpty()) {
                    customerSearchField.setForeground(COL_SUB);
                    customerSearchField.setText("Enter account holder ID...");
                }
            }
        });

        accountRadio.addActionListener(e -> {
            customerSearchField.setEnabled(true);
            customerSearchField.setText("");
            customerSearchField.setForeground(COL_TEXT);
            customerSearchField.requestFocus();
        });
        walkInRadio.addActionListener(e -> {
            customerSearchField.setEnabled(false);
            selectedHolder = null;
            customerNameLabel.setText("Walk-in");
            customerStatusLabel.setText("");
            recalculateTotals();
        });

        panel.add(customerSearchField);
        panel.add(Box.createVerticalStrut(6));

        customerNameLabel   = new JLabel("Walk-in");
        customerStatusLabel = new JLabel("");
        customerNameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        customerNameLabel.setForeground(COL_TEXT);
        customerStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        customerStatusLabel.setForeground(COL_SUB);
        for (JLabel l : new JLabel[]{customerNameLabel, customerStatusLabel}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(l);
        }

        panel.add(Box.createVerticalStrut(16));
        panel.add(makeSeparator());
        panel.add(Box.createVerticalStrut(16));

        // Payment section
        addSectionHeading(panel, "Payment");
        panel.add(Box.createVerticalStrut(10));

        addSmallLabel(panel, "Method:");
        panel.add(Box.createVerticalStrut(4));
        paymentMethodBox = new JComboBox<>(new String[]{"CASH", "CARD", "ACCOUNT"});
        paymentMethodBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        paymentMethodBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        paymentMethodBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentMethodBox.addActionListener(e -> onPaymentMethodChanged());
        panel.add(paymentMethodBox);
        panel.add(Box.createVerticalStrut(10));

        addSmallLabel(panel, "Amount Tendered (£):");
        panel.add(Box.createVerticalStrut(4));
        amountTenderedField = new JTextField("0.00");
        amountTenderedField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        amountTenderedField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        amountTenderedField.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountTenderedField.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER,1,true), new EmptyBorder(6,8,6,8)));
        amountTenderedField.addActionListener(e -> calculateChange());
        panel.add(amountTenderedField);
        panel.add(Box.createVerticalStrut(8));

        changeLabel = new JLabel("Change: £0.00");
        changeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        changeLabel.setForeground(COL_PRIMARY);
        changeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(changeLabel);
        panel.add(Box.createVerticalStrut(10));

        // Card details (hidden unless CARD selected)
        cardDetailsPanel = buildCardDetailsPanel();
        cardDetailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardDetailsPanel.setVisible(false);
        panel.add(cardDetailsPanel);
        panel.add(Box.createVerticalGlue());

        // Buttons
        checkoutBtn = new JButton("Complete Sale");
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        checkoutBtn.setBackground(COL_PRIMARY); checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setBorderPainted(false); checkoutBtn.setFocusPainted(false);
        checkoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        checkoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkoutBtn.addActionListener(e -> completeSale());

        JButton clearBtn = new JButton("Clear Sale");
        clearBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        clearBtn.setForeground(new Color(0xEF4444));
        clearBtn.setBackground(COL_WHITE);
        clearBtn.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xEF4444),1,true), new EmptyBorder(7,12,7,12)));
        clearBtn.setFocusPainted(false);
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        clearBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearBtn.addActionListener(e -> clearSale());

        panel.add(checkoutBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(clearBtn);
        return panel;
    }

    private JPanel buildCardDetailsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(COL_WHITE);

        JLabel lbl = new JLabel("Card Details");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12)); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl); p.add(Box.createVerticalStrut(6));

        cardTypeField  = addCardField(p, "Card Type (e.g. VISA):");
        cardFirst4Field = addCardField(p, "First 4 Digits:");
        cardLast4Field  = addCardField(p, "Last 4 Digits:");
        cardExpiryField = addCardField(p, "Expiry (MM/YY):");
        return p;
    }

    private JTextField addCardField(JPanel panel, String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl); panel.add(Box.createVerticalStrut(3));
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        f.setBorder(new CompoundBorder(new LineBorder(COL_BORDER,1,true), new EmptyBorder(4,8,4,8)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(f); panel.add(Box.createVerticalStrut(6));
        return f;
    }

    // ---------------------------------------------------------------
    // Data loading — wired to DAOs
    // ---------------------------------------------------------------

    private void loadStockItems() {
        SwingWorker<List<StockItem>, Void> w = new SwingWorker<List<StockItem>, Void>() {
            @Override protected List<StockItem> doInBackground() throws Exception {
                return stockDAO.getAllStockItems();
            }
            @Override protected void done() {
                try {
                    stockItems = get();
                    filterProducts();
                } catch (Exception ex) { showDbError(ex); }
            }
        };
        w.execute();
    }

    private void loadVatRate() {
        SwingWorker<BigDecimal, Void> w = new SwingWorker<BigDecimal, Void>() {
            @Override protected BigDecimal doInBackground() throws Exception {
                return merchantSettingsDAO.getVatRate();
            }
            @Override protected void done() {
                try { vatRate = get(); recalculateTotals(); }
                catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    /** CA-06: Lookup account holder by ID typed into customerSearchField */
    private void lookupAccountHolder() {
        String text = customerSearchField.getText().trim();
        if (text.isEmpty()) return;
        try {
            int holderId = Integer.parseInt(text);
            AccountHolder ah = accountHolderDAO.findById(holderId);
            if (ah == null) {
                customerNameLabel.setText("Not found");
                customerStatusLabel.setText("");
                selectedHolder = null;
            } else if (ah.getAccountStatus() == AccountHolder.AccountStatus.SUSPENDED
                    || ah.getAccountStatus() == AccountHolder.AccountStatus.IN_DEFAULT) {
                customerNameLabel.setText(ah.getFullName());
                customerStatusLabel.setText("⚠ " + ah.getAccountStatus() + " — cannot charge to account");
                selectedHolder = null; // don't allow account billing for suspended accounts
            } else {
                selectedHolder = ah;
                customerNameLabel.setText(ah.getFullName());
                customerStatusLabel.setText("Balance: £" +
                    String.format("%.2f", ah.getOutstandingBalance()) +
                    " / Limit: £" + String.format("%.2f", ah.getCreditLimit()));
            }
            recalculateTotals(); // discount may change
        } catch (NumberFormatException ex) {
            customerNameLabel.setText("Invalid ID");
        } catch (SQLException ex) { showDbError(ex); }
    }

    private void filterProducts() {
        String term = productSearchField.getText().trim().toLowerCase();
        if (term.startsWith("search")) term = "";
        productListModel.clear();
        for (StockItem item : stockItems) {
            if (term.isEmpty() || item.getDescription().toLowerCase().contains(term)) {
                productListModel.addElement(item.getStockItemId() + " | "
                    + item.getDescription() + "  £"
                    + String.format("%.2f", item.getRetailPriceExVAT())
                    + "  (qty: " + item.getQuantityAvailable() + ")");
            }
        }
    }

    private void addSelectedToBasket() {
        int selectedIdx = productList.getSelectedIndex();
        if (selectedIdx < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product first.");
            return;
        }

        // Find matching StockItem from the filtered list
        String selectedText = productListModel.getElementAt(selectedIdx);
        int stockItemId = Integer.parseInt(selectedText.split("\\|")[0].trim());
        StockItem item = stockItems.stream()
            .filter(s -> s.getStockItemId() == stockItemId)
            .findFirst().orElse(null);
        if (item == null) return;

        int qty = (int) quantitySpinner.getValue();
        if (qty > item.getQuantityAvailable()) {
            JOptionPane.showMessageDialog(this,
                "Only " + item.getQuantityAvailable() + " packs available.",
                "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal unitPrice = item.getRetailPriceExVAT();
        BigDecimal lineTotal = unitPrice.multiply(new BigDecimal(qty));

        basketModel.addRow(new Object[]{
            item.getDescription(), qty,
            String.format("%.2f", unitPrice),
            String.format("%.2f", lineTotal)
        });
        basketStockItems.add(item);
        basketQuantities.add(qty);
        recalculateTotals();
    }

    private void removeFromBasket() {
        int row = basketTable.getSelectedRow();
        if (row < 0) return;
        basketModel.removeRow(row);
        basketStockItems.remove(row);
        basketQuantities.remove(row);
        recalculateTotals();
    }

    private void recalculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < basketModel.getRowCount(); i++) {
            try {
                subtotal = subtotal.add(new BigDecimal(
                    basketModel.getValueAt(i, 3).toString()));
            } catch (Exception ignored) {}
        }

        // Calculate discount based on account holder's plan
        BigDecimal discount = BigDecimal.ZERO;
        if (selectedHolder != null) {
            try {
                // For FLEXIBLE plans the tier is determined by cumulative monthly spend
                // including THIS purchase — so we add the current basket subtotal
                // to whatever they've already spent this month.
                BigDecimal cumulativeMonthly = selectedHolder.getMonthlyOrderTotal()
                    .add(subtotal);
                discount = discountPlanDAO.calculateDiscount(
                    selectedHolder.getDiscountPlanId(), subtotal, cumulativeMonthly);
            } catch (SQLException ignored) {}
        }

        BigDecimal vatAmount = subtotal.subtract(discount)
            .multiply(vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatAmount).subtract(discount)
            .setScale(2, RoundingMode.HALF_UP);

        subtotalLabel.setText("£" + String.format("%.2f", subtotal));
        vatLabel.setText("£"      + String.format("%.2f", vatAmount));
        discountLabel.setText("£" + String.format("%.2f", discount));
        totalLabel.setText("£"    + String.format("%.2f", total));
    }

    private void calculateChange() {
        try {
            double total    = Double.parseDouble(totalLabel.getText().replace("£",""));
            double tendered = Double.parseDouble(amountTenderedField.getText().trim());
            double change   = tendered - total;
            changeLabel.setText("Change: £" + String.format("%.2f", Math.max(0, change)));
            changeLabel.setForeground(change >= 0 ? COL_PRIMARY : new Color(0xEF4444));
        } catch (NumberFormatException ignored) {}
    }

    private void onPaymentMethodChanged() {
        String method = (String) paymentMethodBox.getSelectedItem();
        cardDetailsPanel.setVisible("CARD".equals(method));
        amountTenderedField.setEnabled("CASH".equals(method));
        changeLabel.setVisible("CASH".equals(method));

        if ("ACCOUNT".equals(method) && selectedHolder == null) {
            JOptionPane.showMessageDialog(this,
                "Please look up an account holder first.", "No Account Selected",
                JOptionPane.WARNING_MESSAGE);
            paymentMethodBox.setSelectedItem("CASH");
        }
    }

    // ---------------------------------------------------------------
    // CA-13 / CA-14 / CA-15 / CA-16 / CA-17: Complete the sale
    // ---------------------------------------------------------------
    private void completeSale() {
        if (basketModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Basket is empty.");
            return;
        }

        String method = (String) paymentMethodBox.getSelectedItem();

        // Validate cash payment
        if ("CASH".equals(method)) {
            try {
                double total    = Double.parseDouble(totalLabel.getText().replace("£",""));
                double tendered = Double.parseDouble(amountTenderedField.getText().trim());
                if (tendered < total) {
                    JOptionPane.showMessageDialog(this,
                        "Amount tendered is less than total due.", "Insufficient Payment",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid amount tendered.");
                return;
            }
        }

        // Validate credit limit for ACCOUNT payments
        if ("ACCOUNT".equals(method) && selectedHolder != null) {
            BigDecimal currentTotal = new BigDecimal(totalLabel.getText().replace("£",""));
            BigDecimal newBalance = selectedHolder.getOutstandingBalance().add(currentTotal);
            if (newBalance.compareTo(selectedHolder.getCreditLimit()) > 0) {
                JOptionPane.showMessageDialog(this,
                    "Cannot complete sale — credit limit exceeded.\n\n"
                    + "Current balance:  £" + String.format("%.2f", selectedHolder.getOutstandingBalance()) + "\n"
                    + "This purchase:    £" + String.format("%.2f", currentTotal) + "\n"
                    + "New balance:      £" + String.format("%.2f", newBalance) + "\n"
                    + "Credit limit:     £" + String.format("%.2f", selectedHolder.getCreditLimit()) + "\n\n"
                    + "Ask the customer to pay their outstanding balance first,\n"
                    + "or take payment by cash or card instead.",
                    "Credit Limit Exceeded", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Build Sale object
        int holderId = selectedHolder != null ? selectedHolder.getHolderId() : 0;
        PaymentMethod pm = PaymentMethod.valueOf(method);
        Sale sale = new Sale(servedByUserId, holderId,
            holderId == 0 ? "Walk-in" : null, pm);

        BigDecimal subtotal  = new BigDecimal(subtotalLabel.getText().replace("£",""));
        BigDecimal vatAmt    = new BigDecimal(vatLabel.getText().replace("£",""));
        BigDecimal discount  = new BigDecimal(discountLabel.getText().replace("£",""));
        BigDecimal total     = new BigDecimal(totalLabel.getText().replace("£",""));

        sale.setSubtotal(subtotal);
        sale.setVatAmount(vatAmt);
        sale.setDiscountAmount(discount);
        sale.setTotalAmount(total);

        if ("CASH".equals(method)) {
            BigDecimal tendered = new BigDecimal(amountTenderedField.getText().trim());
            sale.setPaymentReceived(tendered);
            sale.setChangeGiven(tendered.subtract(total).max(BigDecimal.ZERO));
        }
        if ("CARD".equals(method)) {
            sale.setCardType(cardTypeField.getText().trim());
            sale.setCardFirstFour(cardFirst4Field.getText().trim());
            sale.setCardLastFour(cardLast4Field.getText().trim());
            sale.setCardExpiry(cardExpiryField.getText().trim());
        }

        // Add line items
        for (int i = 0; i < basketStockItems.size(); i++) {
            StockItem si  = basketStockItems.get(i);
            int qty       = basketQuantities.get(i);
            BigDecimal up = si.getRetailPriceExVAT();
            SaleItem item = new SaleItem(0, si.getStockItemId(),
                si.getDescription(), qty, up);
            sale.addItem(item);
        }

        // Save to database
        try {
            AccountHolderDAO ahDAO = new AccountHolderDAO();
            salesDAO.recordSale(sale, stockDAO, ahDAO);

            // Build and show receipt
            String receipt = buildReceiptText(sale);
            ReceiptDialog dialog = new ReceiptDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), receipt);
            dialog.setVisible(true);

            clearSale();
            loadStockItems(); // refresh stock quantities
        } catch (SQLException ex) { showDbError(ex); }
    }

    private void clearSale() {
        basketModel.setRowCount(0);
        basketStockItems.clear();
        basketQuantities.clear();
        selectedHolder = null;
        walkInRadio.setSelected(true);
        customerNameLabel.setText("Walk-in");
        customerStatusLabel.setText("");
        customerSearchField.setEnabled(false);
        amountTenderedField.setText("0.00");
        changeLabel.setText("Change: £0.00");
        paymentMethodBox.setSelectedIndex(0);
        cardDetailsPanel.setVisible(false);
        recalculateTotals();
    }

    private String buildReceiptText(Sale sale) {
        // Try to load the RECEIPT template from the DB
        String template = null;
        try {
            dao.TemplateDAO templateDAO = new dao.TemplateDAO();
            template = templateDAO.getTemplate("RECEIPT");
        } catch (Exception ignored) {}

        // If template loaded, substitute placeholders
        if (template != null && !template.isEmpty()) {
            StringBuilder itemsTable = new StringBuilder();
            for (SaleItem si : sale.getItems()) {
                itemsTable.append(String.format("%-22s %3d x £%.2f = £%.2f%n",
                    si.getDescription(), si.getQuantity(),
                    si.getUnitPrice(), si.getLineTotal()));
            }
            String customerName = sale.getHolderId() > 0
                ? "Account Holder #" + sale.getHolderId()
                : (sale.getOccasionalName() != null ? sale.getOccasionalName() : "Walk-in Customer");

            return template
                .replace("{INVOICE_NO}",    sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "—")
                .replace("{DATE}",          sale.getSaleTimestamp() != null
                    ? sale.getSaleTimestamp().toLocalDateTime().toLocalDate().toString() : "—")
                .replace("{CUSTOMER_NAME}", customerName)
                .replace("{ACCOUNT_NO}",    sale.getHolderId() > 0 ? "ACC" + String.format("%04d", sale.getHolderId()) : "N/A")
                .replace("{ITEMS_TABLE}",   itemsTable.toString().trim())
                .replace("{SUBTOTAL}",      String.format("%.2f", sale.getSubtotal()))
                .replace("{VAT_RATE}",      "0.00")
                .replace("{VAT_AMOUNT}",    String.format("%.2f", sale.getVatAmount()))
                .replace("{TOTAL}",         String.format("%.2f", sale.getTotalAmount()))
                .replace("{PHARMACIST_NAME}", "Cosymed Pharmacy")
                .replace("\\n", "\n");
        }

        // Fallback if template not found
        StringBuilder sb = new StringBuilder();
        sb.append("COSYMED LTD.\n");
        sb.append("Invoice No: ").append(sale.getInvoiceNumber()).append("\n");
        sb.append("----------------------------------------\n\nITEMS:\n");
        for (SaleItem si : sale.getItems()) {
            sb.append(String.format("%-22s %3d x £%.2f = £%.2f%n",
                si.getDescription(), si.getQuantity(),
                si.getUnitPrice(), si.getLineTotal()));
        }
        sb.append("\n----------------------------------------\n");
        sb.append(String.format("Subtotal:  £%.2f%n", sale.getSubtotal()));
        sb.append(String.format("VAT:       £%.2f%n", sale.getVatAmount()));
        sb.append(String.format("Discount:  £%.2f%n", sale.getDiscountAmount()));
        sb.append(String.format("TOTAL:     £%.2f%n", sale.getTotalAmount()));
        sb.append("Payment:   ").append(sale.getPaymentMethod()).append("\n");
        if (sale.getChangeGiven() != null)
            sb.append(String.format("Change:    £%.2f%n", sale.getChangeGiven()));
        sb.append("\nThank you for your custom!\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private void showDbError(Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void addSectionHeading(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Georgia", Font.BOLD, 14));
        l.setForeground(COL_TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
    }

    private void addSmallLabel(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(COL_SUB);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
    }

    private JSeparator makeSeparator() {
        JSeparator s = new JSeparator();
        s.setForeground(COL_BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JLabel makeTotalKey(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(COL_SUB);
        return l;
    }

    private JLabel makeTotalLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(COL_TEXT);
        return l;
    }

    // ---------------------------------------------------------------
    // CA-17: Receipt dialog
    // ---------------------------------------------------------------
    static class ReceiptDialog extends JDialog {
        ReceiptDialog(JFrame parent, String receiptText) {
            super(parent, "Receipt / Invoice", true);
            setSize(420, 500);
            setLocationRelativeTo(parent);

            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.setBorder(new EmptyBorder(16, 16, 16, 16));
            panel.setBackground(Color.WHITE);

            JLabel topLabel = new JLabel("Sale completed.  Receipt generated.");
            topLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            topLabel.setForeground(new Color(0x22C55E));

            JTextArea area = new JTextArea(receiptText);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setEditable(false);
            area.setBackground(Color.WHITE);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(new LineBorder(new Color(0xD6E4DC), 1, true));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnRow.setBackground(Color.WHITE);
            JButton printBtn = new JButton("🖨 Print");
            printBtn.addActionListener(e -> {
                try { area.print(); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage());
                }
            });
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dispose());
            btnRow.add(printBtn); btnRow.add(closeBtn);

            panel.add(topLabel, BorderLayout.NORTH);
            panel.add(scroll,   BorderLayout.CENTER);
            panel.add(btnRow,   BorderLayout.SOUTH);
            setContentPane(panel);
        }
    }
}
