package client;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JTextField portField = new JTextField("8080", 5);
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton connectButton = new JButton("Connect");
    private final JButton loginButton = new JButton("Login");
    private final JButton registerButton = new JButton("Register");
    private final JLabel statusLabel = new JLabel(" ");
    private ChatClient chatClient;

    public static void applyLaf() {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {
        }
    }

    public LoginFrame() {
        super("LTM2 Chat");
        buildUi();
        wireEvents();
        setAuthEnabled(false);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 320);
        setResizable(false);
        setLocationRelativeTo(null);

        // Server row
        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        serverPanel.setBorder(BorderFactory.createTitledBorder("Server"));
        serverPanel.add(new JLabel("Host:"));
        serverPanel.add(hostField);
        serverPanel.add(new JLabel("Port:"));
        serverPanel.add(portField);
        serverPanel.add(connectButton);

        // Account form
        JPanel authPanel = new JPanel(new GridBagLayout());
        authPanel.setBorder(BorderFactory.createTitledBorder("Account"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        authPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        authPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        authPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        authPanel.add(passwordField, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.add(registerButton);
        btnRow.add(loginButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        authPanel.add(btnRow, gbc);

        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 6, 12));

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(serverPanel, BorderLayout.NORTH);
        root.add(authPanel, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void wireEvents() {
        connectButton.addActionListener(e -> handleConnect());
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());
        passwordField.addActionListener(e -> handleLogin());
    }

    private void handleConnect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            showStatus("Invalid port number", true);
            return;
        }
        showStatus("Connecting…", false);
        chatClient = new ChatClient(this::appendStatus);
        chatClient.connect(host, port);
        boolean ok = chatClient.isConnected();
        setAuthEnabled(ok);
        connectButton.setEnabled(!ok);
        if (ok) showStatus("Connected — please log in or register", false);
    }

    private void handleLogin() {
        if (chatClient == null || !chatClient.isConnected()) {
            showStatus("Connect to server first", true);
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean ok = chatClient.login(username, password);
        if (ok) {
            dispose();
            SwingUtilities.invokeLater(() -> new ChatFrame(chatClient, username).setVisible(true));
        }
    }

    private void handleRegister() {
        if (chatClient == null || !chatClient.isConnected()) {
            showStatus("Connect to server first", true);
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        chatClient.register(username, password);
    }

    private void appendStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            boolean isError = text.toLowerCase().contains("fail")
                    || text.toLowerCase().contains("error")
                    || text.toLowerCase().contains("invalid");
            showStatus(text, isError);
        });
    }

    private void showStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.setForeground(isError ? new Color(200, 30, 30) : new Color(0, 120, 0));
    }

    private void setAuthEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }
}
