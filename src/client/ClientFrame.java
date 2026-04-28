package client;

import shared.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

public class ClientFrame extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT));
    private final JTextField usernameField = new JTextField();
    private final JTextField passwordField = new JTextField();
    private final JTextField messageField = new JTextField();
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JButton registerButton = new JButton("Register");
    private final JButton loginButton = new JButton("Login");
    private final JButton logoutButton = new JButton("Logout");
    private final JButton sendButton = new JButton("Send");
    private final JButton imageButton = new JButton("Send Image");
    private final JTextArea statusArea = new JTextArea();
    private final ChatClient chatClient;

    public ClientFrame() {
        super("TCP Chat Client");
        this.chatClient = new ChatClient(this::appendStatus);
        buildUi();
        wireEvents();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JPanel connectionPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        connectionPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(logoutButton);
        connectionPanel.add(new JLabel(""));

        JPanel authPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        authPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        authPanel.add(new JLabel("Username:"));
        authPanel.add(usernameField);
        authPanel.add(new JLabel("Password:"));
        authPanel.add(passwordField);
        authPanel.add(registerButton);
        authPanel.add(loginButton);
        authPanel.add(new JLabel(""));
        authPanel.add(new JLabel(""));

        JPanel messagePanel = new JPanel(new BorderLayout(8, 8));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        messagePanel.add(new JLabel("Message:"), BorderLayout.WEST);
        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        actionPanel.add(sendButton);
        actionPanel.add(imageButton);

        statusArea.setEditable(false);

        add(connectionPanel, BorderLayout.NORTH);
        add(authPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messagePanel, BorderLayout.NORTH);
        bottom.add(actionPanel, BorderLayout.CENTER);
        bottom.add(new JScrollPane(statusArea), BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        connectButton.addActionListener(e -> chatClient.connect(hostField.getText().trim(), Integer.parseInt(portField.getText().trim())));
        disconnectButton.addActionListener(e -> chatClient.disconnect());
        registerButton.addActionListener(e -> auth(true));
        loginButton.addActionListener(e -> auth(false));
        logoutButton.addActionListener(e -> chatClient.logout());
        sendButton.addActionListener(e -> {
            String text = messageField.getText().trim();
            if (!text.isEmpty()) {
                chatClient.sendText(text);
                messageField.setText("");
            }
        });
        imageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                chatClient.sendImage(file);
            }
        });
    }

    private void auth(boolean register) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username và password không được để trống.");
            return;
        }
        boolean success = register
                ? chatClient.register(username, password)
                : chatClient.login(username, password);
        if (!success) {
            return;
        }
        appendStatus(register ? "Register successful" : "Login successful");
    }

    private void appendStatus(String text) {
        statusArea.append(text + "\n");
    }
}
