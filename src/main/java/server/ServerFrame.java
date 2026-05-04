package server;

import shared.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class ServerFrame extends JFrame {
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT));
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JLabel statusLabel = new JLabel("Stopped");
    private final JTextArea logArea = new JTextArea();
    private final JTextArea messageArea = new JTextArea();
    private final ChatServer server;

    public ServerFrame() {
        super("TCP Chat Server");
        this.server = new ChatServer(this::appendLog, this::appendMessage, this::setStatus);
        buildUi();
        wireEvents();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel controlsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        controlsPanel.add(new JLabel("Port:"));
        controlsPanel.add(portField);
        controlsPanel.add(startButton);
        controlsPanel.add(stopButton);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        header.add(statusPanel, BorderLayout.NORTH);
        header.add(controlsPanel, BorderLayout.SOUTH);

        logArea.setEditable(false);
        messageArea.setEditable(false);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        center.add(new JScrollPane(logArea));
        center.add(new JScrollPane(messageArea));

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private void wireEvents() {
        startButton.addActionListener(e -> handleStart());
        stopButton.addActionListener(e -> server.stop());
    }

    private void handleStart() {
        String raw = portField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(raw);
            if (port < 1 || port > 65535) throw new NumberFormatException("out of range");
        } catch (NumberFormatException ex) {
            setStatus("Invalid port — enter 1–65535");
            return;
        }
        server.start(port);
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> logArea.append(text + "\n"));
    }

    private void appendMessage(String text) {
        SwingUtilities.invokeLater(() -> messageArea.append(text + "\n"));
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}
