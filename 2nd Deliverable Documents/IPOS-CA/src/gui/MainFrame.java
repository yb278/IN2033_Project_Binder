package gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import models.User;
import service.AccountStatusService;
import service.CrossSystemService;

/**
 * MainFrame
 *
 * The main application window shown after a successful login.
 * Contains a left-hand navigation sidebar and a content area on the right.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class MainFrame extends JFrame {

    // ---------------------------------------------------------------
    // Colours
    // ---------------------------------------------------------------
    private static final Color COL_SIDEBAR       = new Color(0xF8FAF9);   // light grey-white
    private static final Color COL_SIDEBAR_HOVER = new Color(0xE8F5EE);   // light green tint on hover
    private static final Color COL_SIDEBAR_SEL   = new Color(0xD4EDDA);   // selected = soft green
    private static final Color COL_SIDEBAR_TEXT  = new Color(0x1C2B20);   // near-black — readable on light bg
    private static final Color COL_SIDEBAR_SUB   = new Color(0x6B7C72);   // muted for section labels
    private static final Color COL_TOPBAR        = Color.WHITE;
    private static final Color COL_CONTENT_BG    = new Color(0xF5F7FA);
    private static final Color COL_BORDER        = new Color(0xD6E4DC);
    private static final Color COL_ACCENT        = new Color(0xE8F5EE);

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------
    private final String loggedInUser;
    private final String role;
    private final User   currentUser;
    private JPanel       contentArea;
    private JButton      currentNavButton;
    private JLabel       pageTitleLabel;

    public MainFrame(String displayName, String role, User currentUser) {
        this.loggedInUser = displayName;
        this.role         = role;
        this.currentUser  = currentUser;

        setTitle("IPOS-CA — Cosymed Pharmacy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);

        // Run account status check on every login (Fig.1 state machine)
        runAccountStatusCheck();
    }

    // ---------------------------------------------------------------
    // Build layout
    // ---------------------------------------------------------------

    private void initComponents() {
        setLayout(new BorderLayout());

        // Top bar
        add(buildTopBar(), BorderLayout.NORTH);

        // Sidebar + content split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildContentArea());
        split.setDividerSize(0);
        split.setDividerLocation(210);
        split.setEnabled(false);
        add(split, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Top bar
    // ---------------------------------------------------------------

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COL_TOPBAR);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, COL_BORDER),
            new EmptyBorder(10, 20, 10, 20)
        ));
        bar.setPreferredSize(new Dimension(0, 54));

        // Left — page title (updated when nav changes)
        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(new Font("Georgia", Font.BOLD, 17));
        pageTitleLabel.setForeground(new Color(0x1C2B20));
        bar.add(pageTitleLabel, BorderLayout.WEST);

        // Right — user info + logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(COL_TOPBAR);

        // Role badge
        JLabel roleBadge = new JLabel(role);
        roleBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        roleBadge.setForeground(new Color(0x1A6B3C));
        roleBadge.setBackground(new Color(0xD4EDDA));
        roleBadge.setOpaque(true);
        roleBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        rightPanel.add(roleBadge);

        // Username
        JLabel userLabel = new JLabel(loggedInUser);
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        userLabel.setForeground(new Color(0x1C2B20));
        rightPanel.add(userLabel);

        // Logout button
        JButton logoutBtn = new JButton("Sign Out");
        logoutBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        logoutBtn.setForeground(new Color(0x1A6B3C));
        logoutBtn.setBackground(COL_ACCENT);
        logoutBtn.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        rightPanel.add(logoutBtn);

        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // Sidebar
    // ---------------------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COL_SIDEBAR);

        // Pharmacy name at top
        JPanel brandPanel = new JPanel(new BorderLayout());
        brandPanel.setBackground(COL_SIDEBAR);
        brandPanel.setBorder(new EmptyBorder(18, 14, 14, 14));
        brandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JLabel brandName = new JLabel("Cosymed Ltd.");
        brandName.setFont(new Font("Georgia", Font.BOLD, 14));
        brandName.setForeground(new Color(0x1A6B3C));   // pharmacy green on white
        brandPanel.add(brandName, BorderLayout.CENTER);

        JLabel brandSub = new JLabel("IPOS Client Application");
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        brandSub.setForeground(new Color(0x6B7C72));
        brandPanel.add(brandSub, BorderLayout.SOUTH);

        sidebar.add(brandPanel);
        sidebar.add(makeSidebarDivider());
        sidebar.add(Box.createVerticalStrut(6));

        // ---- Nav items based on role ----
        if (role.equals("ADMIN")) {
            sidebar.add(makeSectionLabel("Administration"));
            sidebar.add(makeNavButton("User Management",  "UserManagement",  true));
            sidebar.add(Box.createVerticalStrut(4));
        }

        if (role.equals("ADMIN") || role.equals("PHARMACIST")) {
            sidebar.add(makeSectionLabel("Operations"));
            sidebar.add(makeNavButton("Point of Sale",    "PointOfSale",     role.equals("PHARMACIST")));
            sidebar.add(makeNavButton("Account Holders",  "AccountHolders",  false));
            sidebar.add(makeNavButton("Stock",            "Stock",           false));
            sidebar.add(makeNavButton("Orders (SA)",      "Orders",          false));
            sidebar.add(makeNavButton("Online Sales (PU)","OnlineSales",     false));
            sidebar.add(makeNavButton("Statements",       "Statements",      false));
            sidebar.add(Box.createVerticalStrut(4));
        }

        if (role.equals("MANAGER") || role.equals("ADMIN")) {
            sidebar.add(makeSectionLabel("Management"));
            sidebar.add(makeNavButton("Reports",          "Reports",         role.equals("MANAGER")));
            sidebar.add(makeNavButton("Credit & Discounts","CreditDiscounts",false));
            sidebar.add(makeNavButton("Templates",        "Templates",       false));
            sidebar.add(makeNavButton("Settings",         "Settings",        false));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel versionLabel = new JLabel("IN2033 · Team B · 2025-26");
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(0x6B7C72));
        versionLabel.setBorder(new EmptyBorder(10, 14, 14, 14));
        sidebar.add(versionLabel);

        // Wrap in a scroll pane so items never get cut off on small screens
        JScrollPane scroll = new JScrollPane(sidebar,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(210, 0));
        scroll.setMinimumSize(new Dimension(210, 0));
        scroll.getViewport().setBackground(COL_SIDEBAR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(210, 0));
        wrapper.setBorder(new MatteBorder(0, 0, 0, 1, new Color(0xD6E4DC)));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ---------------------------------------------------------------
    // Content area
    // ---------------------------------------------------------------

    private JPanel buildContentArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COL_CONTENT_BG);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(COL_CONTENT_BG);
        contentArea.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Default landing panel
        contentArea.add(buildLandingPanel(), BorderLayout.CENTER);

        wrapper.add(contentArea, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Simple welcome panel shown on first load.
     * Summarises quick stats — will be populated from DB later.
     */
    private JPanel buildLandingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COL_CONTENT_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);

        // Welcome message
        JLabel welcome = new JLabel("Welcome back, " + loggedInUser + ".");
        welcome.setFont(new Font("Georgia", Font.BOLD, 22));
        welcome.setForeground(new Color(0x1C2B20));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 3;
        gc.anchor = GridBagConstraints.WEST;
        panel.add(welcome, gc);

        JLabel sub = new JLabel("Select a section from the sidebar to get started.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(0x6B7C72));
        gc.gridy = 1;
        panel.add(sub, gc);

        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1; gc.weighty = 1;

        // Quick-stat cards — static placeholders (wire to DB later)
        panel.add(buildStatCard("Account Holders", "6",     "Total registered",       new Color(0xA78BFA)), gc);
        gc.gridx = 1;
        panel.add(buildStatCard("Stock Items",     "16",    "Items in inventory",      new Color(0x34D399)), gc);
        gc.gridx = 2;
        panel.add(buildStatCard("Low Stock",       "2",     "Items need reordering",   new Color(0xFB923C)), gc);

        gc.gridx = 0; gc.gridy = 3;
        panel.add(buildStatCard("Pending Orders",  "3",     "Orders to InfoPharma",    new Color(0xF472B6)), gc);
        gc.gridx = 1;
        panel.add(buildStatCard("Sales Today",     "0",     "Transactions recorded",   new Color(0x60A5FA)), gc);
        gc.gridx = 2;
        panel.add(buildStatCard("Overdue Accounts","2",     "Accounts need attention", new Color(0xF87171)), gc);

        return panel;
    }

    private JPanel buildStatCard(String title, String value, String sub, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true),
            new EmptyBorder(16, 18, 16, 18)
        ));

        // Coloured top stripe
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(0, 4));
        card.add(stripe, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Georgia", Font.BOLD, 30));
        valueLabel.setForeground(new Color(0x1C2B20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(new Color(0x1C2B20));

        JLabel subLabel = new JLabel(sub);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subLabel.setForeground(new Color(0x6B7C72));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(valueLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subLabel);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------------------------
    // Navigation switching
    // ---------------------------------------------------------------

    /**
     * Swaps the content panel when a nav button is clicked.
     * Each panel class is instantiated fresh — wire DAO calls inside each panel.
     */
    private void showPanel(String panelName, String pageTitle, JButton navButton) {
        // Update nav selection highlight
        if (currentNavButton != null) {
            currentNavButton.setBackground(COL_SIDEBAR);
        }
        navButton.setBackground(COL_SIDEBAR_SEL);
        currentNavButton = navButton;

        // Update page title
        pageTitleLabel.setText(pageTitle);

        // Swap content
        contentArea.removeAll();

        JPanel panel;
        switch (panelName) {
            case "PointOfSale":     panel = new PointOfSalePanel();      break;
            case "AccountHolders":  panel = new AccountHoldersPanel();   break;
            case "Stock":           panel = new StockPanel();             break;
            case "Orders":          panel = new OrdersPanel();            break;
            case "OnlineSales":     panel = new OnlineSalesPanel();       break;
            case "Statements":      panel = new StatementsPanel();        break;
            case "UserManagement":  panel = new UserManagementPanel();    break;
            case "Reports":         panel = new ReportsPanel();           break;
            case "CreditDiscounts": panel = new CreditDiscountsPanel();   break;
            case "Templates":       panel = new TemplatesPanel();         break;
            case "Settings":        panel = new SettingsPanel();          break;
            default:                panel = buildLandingPanel();          break;
        }

        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    // ---------------------------------------------------------------
    // Account status auto-check (runs in background on every login)
    // ---------------------------------------------------------------

    /**
     * Runs the AccountStatusService in a background thread on login.
     * This updates NORMAL → SUSPENDED → IN_DEFAULT as needed per Fig.1.
     * Runs silently — errors are logged to console but don't crash the app.
     */
    private void runAccountStatusCheck() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    // 1. Update account statuses (Fig.1 state machine)
                    AccountStatusService statusService = new AccountStatusService();
                    statusService.runStatusCheck();
                    statusService.resetMonthlyTotalsIfNewMonth();
                    System.out.println("[MainFrame] Account status check complete.");

                    // 2. Process any pending online sales from IPOS-PU
                    CrossSystemService crossSystem = new CrossSystemService();
                    if (crossSystem.isIposPuReachable()) {
                        int processed = crossSystem.processOnlineSales();
                        if (processed > 0) {
                            SwingUtilities.invokeLater(() ->
                                showStatusMessage(processed + " online sale(s) synced from IPOS-PU."));
                        }
                    } else {
                        System.out.println("[MainFrame] IPOS-PU not reachable — skipping online sales sync.");
                    }
                } catch (Exception e) {
                    System.err.println("[MainFrame] Startup check failed: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    /** Shows a brief status message in the top bar */
    private void showStatusMessage(String message) {
        JLabel msg = new JLabel("  ✓ " + message);
        msg.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msg.setForeground(new Color(0x1A6B3C));
        pageTitleLabel.setText(pageTitleLabel.getText()); // keep title

        // Add message to top bar temporarily
        Container topBar = pageTitleLabel.getParent();
        topBar.add(msg, BorderLayout.CENTER);
        topBar.revalidate();

        // Remove after 4 seconds
        Timer timer = new Timer(4000, e -> {
            topBar.remove(msg);
            topBar.revalidate();
            topBar.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    // ---------------------------------------------------------------
    // Logout
    // ---------------------------------------------------------------

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to sign out?",
            "Sign Out",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginFrame();
            dispose();
        }
    }

    // ---------------------------------------------------------------
    // Helper UI builders
    // ---------------------------------------------------------------

    private JButton makeNavButton(String label, String panelName, boolean selectByDefault) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(COL_SIDEBAR_TEXT);
        btn.setBackground(selectByDefault ? COL_SIDEBAR_SEL : COL_SIDEBAR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        // Green left-bar accent on selected item, invisible otherwise
        btn.setBorder(selectByDefault
            ? new CompoundBorder(new MatteBorder(0, 3, 0, 0, new Color(0x4ADE80)),
                                 new EmptyBorder(9, 12, 9, 8))
            : new CompoundBorder(new MatteBorder(0, 3, 0, 0, COL_SIDEBAR),
                                 new EmptyBorder(9, 12, 9, 8)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setToolTipText(label);

        String pageTitle = label;

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn != currentNavButton) btn.setBackground(COL_SIDEBAR_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn != currentNavButton) btn.setBackground(COL_SIDEBAR);
            }
        });

        btn.addActionListener(e -> {
            // Clear accent on previous selection
            if (currentNavButton != null) {
                currentNavButton.setBackground(COL_SIDEBAR);
                currentNavButton.setBorder(new CompoundBorder(
                    new MatteBorder(0, 3, 0, 0, COL_SIDEBAR),
                    new EmptyBorder(9, 12, 9, 8)));
            }
            // Apply accent to this button
            btn.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, new Color(0x4ADE80)),
                new EmptyBorder(9, 12, 9, 8)));
            showPanel(panelName, pageTitle, btn);
        });

        if (selectByDefault) {
            SwingUtilities.invokeLater(() -> {
                currentNavButton = btn;
                showPanel(panelName, pageTitle, btn);
            });
        }

        return btn;
    }

    /** Returns a plain-text icon for each panel — renders reliably inside HTML nobr */
    private String getNavIcon(String panelName) { return ""; } // icons removed, using accent bar instead

    private JLabel makeSectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(COL_SIDEBAR_SUB);
        label.setBorder(new EmptyBorder(12, 14, 4, 14));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return label;
    }

    private JSeparator makeSidebarDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xD6E4DC));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ---------------------------------------------------------------
    // PlaceholderPanel — shown until each real panel is built
    // ---------------------------------------------------------------

    /**
     * Temporary placeholder panel shown while individual screens are being built.
     * Replace each one with the real panel class as they are completed.
     */
    static class PlaceholderPanel extends JPanel {
        PlaceholderPanel(String title, String useCases, Color accentColor) {
            setLayout(new GridBagLayout());
            setBackground(new Color(0xF5F7FA));

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD6E4DC), 1, true),
                new EmptyBorder(40, 48, 40, 48)
            ));

            // Coloured accent bar at top
            JPanel stripe = new JPanel();
            stripe.setBackground(accentColor);
            stripe.setPreferredSize(new Dimension(420, 5));
            stripe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
            stripe.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(stripe);
            card.add(Box.createVerticalStrut(28));

            JLabel icon = new JLabel("🚧");
            icon.setFont(new Font("SansSerif", Font.PLAIN, 36));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(icon);
            card.add(Box.createVerticalStrut(16));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
            titleLabel.setForeground(new Color(0x1C2B20));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(8));

            JLabel buildingLabel = new JLabel("This panel is being built.");
            buildingLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            buildingLabel.setForeground(new Color(0x6B7C72));
            buildingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(buildingLabel);
            card.add(Box.createVerticalStrut(20));

            JLabel ucLabel = new JLabel("Use cases: " + useCases);
            ucLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            ucLabel.setForeground(new Color(0x94A3A0));
            ucLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(ucLabel);

            add(card);
        }
    }
}
