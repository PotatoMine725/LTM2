package client;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

public class ClientFrame extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("8080");
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
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
        setSize(1000, 680);
        setLocationRelativeTo(null);

        JPanel connectionPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection"));
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(logoutButton);
        connectionPanel.add(new JLabel(""));

        JPanel authPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        authPanel.setBorder(BorderFactory.createTitledBorder("Account"));
        authPanel.add(new JLabel("Username:"));
        authPanel.add(usernameField);
        authPanel.add(new JLabel("Password:"));
        authPanel.add(passwordField);
        authPanel.add(registerButton);
        authPanel.add(loginButton);
        authPanel.add(new JLabel(""));
        authPanel.add(new JLabel(""));

        JPanel messagePanel = new JPanel(new BorderLayout(8, 8));
        messagePanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        JPanel filePanel = new JPanel(new BorderLayout(8, 8));
        filePanel.setBorder(BorderFactory.createTitledBorder("File Transfer"));
        filePanel.add(imageButton, BorderLayout.WEST);

        statusArea.setEditable(false);

        JPanel top = new JPanel(new GridLayout(2, 1, 8, 8));
        top.add(connectionPanel);
        top.add(authPanel);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.add(messagePanel);
        center.add(filePanel);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);
    }

    private void wireEvents() {
        connectButton.addActionListener(e -> chatClient.connect(hostField.getText().trim(), Integer.parseInt(portField.getText().trim())));
        disconnectButton.addActionListener(e -> chatClient.disconnect());
        registerButton.addActionListener(e -> handleRegister());
        loginButton.addActionListener(e -> handleLogin());
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

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean ok = chatClient.register(username, password);
        appendStatus(ok ? "Register success" : "Register failed");
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean ok = chatClient.login(username, password);
        appendStatus(ok ? "Login success" : "Login failed");
    }

    private void appendStatus(String text) {
        statusArea.append(text + "\n");
    }
}
