package gui;

import dao.UserDAO;
import models.User;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginFrame — CA-01 Login
 * Authenticates against the users table via UserDAO.
 * Passes the full User object to MainFrame on success.
 *
 * IN2033 Team Project 2025-2026 – Team B (IPOS-CA)
 */
public class LoginFrame extends JFrame {

    private static final Color COL_BG        = new Color(0xF5F7FA);
    private static final Color COL_CARD      = Color.WHITE;
    private static final Color COL_PRIMARY   = new Color(0x1A6B3C);
    private static final Color COL_PRIMARY_H = new Color(0x14522E);
    private static final Color COL_SUBTEXT   = new Color(0x6B7C72);
    private static final Color COL_BORDER    = new Color(0xD6E4DC);
    private static final Color COL_TEXT      = new Color(0x1C2B20);
    private static final Color COL_ERROR     = new Color(0xC0392B);
    private static final Color COL_ERROR_BG  = new Color(0xFDECEA);

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;
    private JLabel         errorLabel;
    private JCheckBox      showPasswordBox;

    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("IPOS-CA — InfoPharma");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(COL_BG);
        setContentPane(root);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COL_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true),
            new EmptyBorder(40, 44, 40, 44)
        ));
        card.setPreferredSize(new Dimension(360, 460));

        // Green cross icon
        JPanel iconCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COL_PRIMARY);
                g2.fillOval(0, 0, 52, 52);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(26, 14, 26, 38);
                g2.drawLine(14, 26, 38, 26);
            }
        };
        iconCircle.setPreferredSize(new Dimension(52, 52));
        iconCircle.setBackground(COL_CARD);

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoPanel.setBackground(COL_CARD);
        logoPanel.add(iconCircle);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logoPanel);
        card.add(Box.createVerticalStrut(16));

        JLabel titleLabel = new JLabel("IPOS-CA");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 24));
        titleLabel.setForeground(COL_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitleLabel = new JLabel("InfoPharma Client Application");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(COL_SUBTEXT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(32));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(28));

        card.add(makeFieldLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        usernameField = makeTextField("Enter your username");
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));

        card.add(makeFieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        passwordField = new JPasswordField();
        styleTextField(passwordField, "Enter your password");
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));

        showPasswordBox = new JCheckBox("Show password");
        showPasswordBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        showPasswordBox.setForeground(COL_SUBTEXT);
        showPasswordBox.setBackground(COL_CARD);
        showPasswordBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPasswordBox.addActionListener(e ->
            passwordField.setEchoChar(showPasswordBox.isSelected() ? (char) 0 : '•'));
        card.add(showPasswordBox);
        card.add(Box.createVerticalStrut(24));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorLabel.setForeground(COL_ERROR);
        errorLabel.setOpaque(true);
        errorLabel.setBackground(COL_CARD);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(12));

        loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(COL_PRIMARY);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { loginButton.setBackground(COL_PRIMARY_H); }
            @Override public void mouseExited(MouseEvent e)  { loginButton.setBackground(COL_PRIMARY); }
        });
        loginButton.addActionListener(e -> attemptLogin());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        card.add(loginButton);
        card.add(Box.createVerticalStrut(20));

        JLabel versionLabel = new JLabel("IN2033 Team Project 2025–26 · Team B");
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(0xBBBBBB));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(versionLabel);

        root.add(card);
    }

    // ---------------------------------------------------------------
    // Login — wired to UserDAO
    // ---------------------------------------------------------------
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        clearError();

        // Run DB call on background thread so GUI never freezes
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return userDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        // Short delay for UX feel
                        Timer timer = new Timer(400, e -> {
                            new MainFrame(user.getFullName(), user.getRole().name(), user);
                            dispose();
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        showError("Invalid username or password. Please try again.");
                        passwordField.setText("");
                        passwordField.requestFocus();
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");
                    }
                } catch (Exception ex) {
                    showError("Database error — check MySQL is running.");
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                }
            }
        };
        worker.execute();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(COL_TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField();
        styleTextField(f, placeholder);
        return f;
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(COL_SUBTEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText(""); field.setForeground(COL_TEXT);
                }
                field.setBorder(new CompoundBorder(
                    new LineBorder(COL_PRIMARY, 1, true), new EmptyBorder(8, 12, 8, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(COL_SUBTEXT); field.setText(placeholder);
                }
                field.setBorder(new CompoundBorder(
                    new LineBorder(COL_BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));
            }
        });
    }

    private JSeparator makeDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(COL_BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setForeground(COL_ERROR);
        errorLabel.setBackground(COL_ERROR_BG);
        errorLabel.setOpaque(true);
        errorLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    private void clearError() {
        errorLabel.setText(" ");
        errorLabel.setOpaque(false);
        errorLabel.setBorder(null);
    }
}
